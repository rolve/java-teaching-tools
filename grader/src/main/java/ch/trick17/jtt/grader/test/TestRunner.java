package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestRunner {

    private static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

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
            var config = mapper.readValue(in, TestRunConfig.class);

            var result = new TestRun(config).execute();

            var out = result.toJson();
            socket.getOutputStream().write(out.getBytes(UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
