package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.testrunner.forkedvm.Result.ReturnedValue;
import ch.trick17.jtt.testrunner.forkedvm.Result.ThrownException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ForkedVmServer {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(new ForkedVmModule())
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, JAVA_LANG_OBJECT);
    }

    public static void main(String[] args) throws IOException {
        var server = new ServerSocket(0);
        System.out.println(server.getLocalPort()); // read by the parent process
        System.out.flush();

        while (true) {
            var socket = server.accept();
            new Thread(() -> handle(socket)).start();
        }
    }

    private static void handle(Socket socket) {
        try (socket) {
            var in = new Scanner(socket.getInputStream(), UTF_8).nextLine();
            var call = mapper.readValue(in, MethodCall.class);

            Result result;
            try {
                var cls = findClass(call.className());
                var paramTypes = new ArrayList<Class<?>>();
                for (var typeName : call.paramTypeNames()) {
                    paramTypes.add(findClass(typeName));
                }
                var method = cls.getDeclaredMethod(call.methodName(),
                        paramTypes.toArray(Class<?>[]::new));
                method.setAccessible(true);
                var args = call.args().toArray();

                result = new ReturnedValue(method.invoke(null, args));
            } catch (InvocationTargetException e) {
                result = new ThrownException(e.getCause());
            } catch (ReflectiveOperationException e) {
                result = new ThrownException(e);
            }

            mapper.writeValue(socket.getOutputStream(), result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Class<?> findClass(String className) throws ClassNotFoundException {
        return switch (className) {
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "boolean" -> boolean.class;
            case "char" -> char.class;
            default -> Class.forName(className);
        };
    }
}
