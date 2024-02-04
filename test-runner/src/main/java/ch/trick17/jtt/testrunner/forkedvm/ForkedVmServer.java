package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ForkedVmServer {

    private static final ObjectMapper mapper;

    static {
        var module = new SimpleModule("InMemClassFileModule", new Version(1, 0, 0, null, null, null));
        module.addDeserializer(InMemClassFile.class, new InMemClassFileDeserializer());
        mapper = new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(module)
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                        JAVA_LANG_OBJECT, PROPERTY);
    }

    public static void main(String[] args) throws Exception {
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

            var cls = findClass(call.className());
            var parameterTypes = call.paramTypeNames().stream()
                    .map(ForkedVmServer::findClass)
                    .toArray(Class<?>[]::new);
            var method = cls.getMethod(call.methodName(), parameterTypes);

            var result = method.invoke(null, call.args().toArray());

            var out = mapper.writeValueAsString(result);
            socket.getOutputStream().write(out.getBytes(UTF_8));
        } catch (IOException | InvocationTargetException |
                 IllegalAccessException | NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static Class<?> findClass(String className) {
        try {
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
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
