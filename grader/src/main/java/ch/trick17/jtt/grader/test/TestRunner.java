package ch.trick17.jtt.grader.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.io.InputStream.nullInputStream;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.Integer.getInteger;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

public class TestRunner {

    private static PrintStream err;

    public static void main(String[] args) throws IOException {
        var server = new ServerSocket(0);
        System.out.println(server.getLocalPort()); // read by the parent process
        System.out.flush();

        err = System.err;
        System.setIn(nullInputStream());
        System.setOut(new PrintStream(nullOutputStream()));
        System.setErr(new PrintStream(nullOutputStream()));
        currentThread().setUncaughtExceptionHandler((t, e) -> e.printStackTrace(err));

        while (true) {
            var socket = server.accept();
            new Thread(() -> handle(socket)).start();
        }
    }

    private static void handle(Socket socket) {
        try (socket) {
            var in = new Scanner(socket.getInputStream(), UTF_8).nextLine();
            var config = TestRunConfig.fromJson(in);

            var result = new TestRun(config).execute();

            var out = result.toJson();
            socket.getOutputStream().write(out.getBytes(UTF_8));
        } catch (Throwable t) {
            t.printStackTrace(err);
        }
    }
}
