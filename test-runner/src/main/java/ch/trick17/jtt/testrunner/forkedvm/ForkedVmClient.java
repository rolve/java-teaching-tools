package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;
import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;

public class ForkedVmClient implements Closeable {

    private static final int CONNECT_TRIES = 3;

    private final ObjectMapper mapper;
    private final List<String> vmArgs;

    private Process forkedVm;
    private int port;

    public ForkedVmClient() {
        this(emptyList());
    }

    public ForkedVmClient(List<String> vmArgs) {
        this.vmArgs = copyOf(vmArgs);

        var module = new SimpleModule("ForkedVmModule", new Version(1, 0, 0, null, null, null));
        module.addSerializer(InMemClassFile.class, new InMemClassFileSerializer());
        module.addDeserializer(Throwable.class, new ThrowableDeserializer());
        mapper = new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(module)
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                        JAVA_LANG_OBJECT, PROPERTY);
    }

    public List<String> getVmArgs() {
        return vmArgs;
    }

    private synchronized void ensureForkedVmRunning() {
        if (forkedVm == null || !forkedVm.isAlive()) {
            try {
                forkedVm = new JavaProcessBuilder(ForkedVmServer.class)
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
                var request = mapper.writeValueAsString(call) + "\n";
                socket.getOutputStream().write(request.getBytes(UTF_8));
                var response = new String(socket.getInputStream().readAllBytes(), UTF_8);
                return mapper.readValue(response, returnType);
            } catch (IOException e) {
                if (tries == CONNECT_TRIES) {
                    throw e;
                } // else try again
            }
            killForkedVm();
        }
    }

    private void killForkedVm() {
        if (forkedVm != null && forkedVm.isAlive()) {
            forkedVm.destroyForcibly();
        }
    }

    @Override
    public void close() {
        killForkedVm();
    }
}
