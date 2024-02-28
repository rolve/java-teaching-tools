package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;
import ch.trick17.jtt.testrunner.forkedvm.Result.ReturnedValue;
import ch.trick17.jtt.testrunner.forkedvm.Result.ThrownException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;

public class ForkedVmClient implements Closeable {

    private static final int CONNECT_TRIES = 3;

    private final List<String> vmArgs;
    private final List<String> moduleClasses = new ArrayList<>();
    private final ObjectMapper mapper;

    private Process forkedVm;
    private int port;

    public ForkedVmClient() {
        this(emptyList(), emptyList());
    }

    public ForkedVmClient(List<String> vmArgs, Iterable<Class<? extends Module>> moduleClasses) {
        this.vmArgs = copyOf(vmArgs);

        var modules = new ArrayList<Module>();
        for (var cls : moduleClasses) {
            this.moduleClasses.add(cls.getName());
            try {
                modules.add(cls.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("could not instantiate module " + cls.getName(), e);
            }
        }
        mapper = new ObjectMapper()
                .findAndRegisterModules()
                .registerModules(modules)
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, JAVA_LANG_OBJECT);
    }

    public List<String> getVmArgs() {
        return vmArgs;
    }

    private synchronized void ensureForkedVmRunning() {
        if (forkedVm == null || !forkedVm.isAlive()) {
            try {
                forkedVm = new JavaProcessBuilder(ForkedVmServer.class, moduleClasses)
                        .vmArgs("-XX:-OmitStackTraceInFastThrow")
                        .addVmArgs(vmArgs.toArray(String[]::new))
                        .autoExit(true)
                        .start();
                var copier = new Thread(new LineCopier(forkedVm.getErrorStream(),
                        new LineWriterAdapter(System.out)));
                copier.setDaemon(true);
                copier.start();
                port = new Scanner(forkedVm.getInputStream()).nextInt();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    public <R> R runInForkedVm(Class<?> cls, String methodName,
                               List<?> args, Class<R> returnType) throws IOException {
        var paramTypes = args.stream()
                .map(o -> typeOf(o))
                .collect(toList());
        return runInForkedVm(cls, methodName, paramTypes, args, returnType);
    }

    private static Class<?> typeOf(Object o) {
        if (o instanceof Byte) {
            return byte.class;
        } else if (o instanceof Short) {
            return short.class;
        } else if (o instanceof Integer) {
            return int.class;
        } else if (o instanceof Long) {
            return long.class;
        } else if (o instanceof Float) {
            return float.class;
        } else if (o instanceof Double) {
            return double.class;
        } else if (o instanceof Boolean) {
            return boolean.class;
        } else if (o instanceof Character) {
            return char.class;
        } else {
            return o.getClass();
        }
    }

    public <R> R runInForkedVm(Class<?> cls, String methodName,
                               List<? extends Class<?>> paramTypes, List<?> args,
                               Class<R> returnType) throws IOException {
        var paramTypeNames = paramTypes.stream()
                .map(Class::getName)
                .collect(toList());
        return runInForkedVm(cls.getName(), methodName, paramTypeNames, args, returnType);
    }

    public <R> R runInForkedVm(String className, String methodName,
                               List<String> paramTypeNames, List<?> args,
                               Class<R> returnType) throws IOException {
        var call = new MethodCall(className, methodName, paramTypeNames, args);
        return runInForkedVm(call, returnType);
    }

    public <R> R runInForkedVm(MethodCall call, Class<R> returnType) throws IOException {
        for (int tries = 1; ; tries++) {
            ensureForkedVmRunning();
            try (var socket = new Socket("localhost", port)) {
                var request = mapper.writeValueAsBytes(call);
                socket.getOutputStream().write(request);
                socket.getOutputStream().write('\n');

                var result = mapper.readValue(socket.getInputStream(), Result.class);
                if (result instanceof ReturnedValue v) {
                    return returnType.cast(v.value());
                } else if (result instanceof ThrownException e) {
                    rethrow(e.exception(), call);
                }
            } catch (IOException | OutOfMemoryError e) { // includes exceptions from the server
                if (tries == CONNECT_TRIES) {
                    throw e;
                } // else try again
            }
            killForkedVm();
        }
    }

    private void rethrow(Throwable exception, MethodCall call) throws IOException {
        adaptStackTrace(exception, call);
        if(exception instanceof RuntimeException e) {
            throw e;
        } else if (exception instanceof Error e) {
            throw e;
        } else if (exception instanceof IOException e) {
            throw e;
        } else {
            throw new RuntimeException(exception);
        }
    }

    private static void adaptStackTrace(Throwable exception, MethodCall call) {
        var trace = new ArrayList<>(asList(exception.getStackTrace()));
        for (var i = trace.listIterator(trace.size()); i.hasPrevious(); ) {
            var prev = i.previous();
            if (prev.getMethodName().equals(call.methodName()) &&
                prev.getClassName().equals(call.className())) {
                break;
            } else {
                i.remove();
            }
        }
        stream(new Error().getStackTrace())
                .dropWhile(s -> s.getClassName().equals(ForkedVmClient.class.getName()))
                .forEach(trace::add);
        exception.setStackTrace(trace.toArray(StackTraceElement[]::new));
    }

    private synchronized void killForkedVm() {
        if (forkedVm != null && forkedVm.isAlive()) {
            forkedVm.destroyForcibly();
        }
    }

    @Override
    public void close() {
        killForkedVm();
    }
}
