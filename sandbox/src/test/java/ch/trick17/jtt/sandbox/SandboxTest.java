package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.sandbox.SandboxResult.Kind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static ch.trick17.jtt.sandbox.InputMode.CLOSED;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.*;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class SandboxTest {

    // Many features are tested indirectly in GraderTest, not here

    private static ByteArrayInputStream inSupplier;
    private static ByteArrayOutputStream outRecorder;
    private static ByteArrayOutputStream errRecorder;

    @BeforeAll
    public static void install() {
        inSupplier = new ByteArrayInputStream("Hello, World!".getBytes());
        outRecorder = new ByteArrayOutputStream();
        errRecorder = new ByteArrayOutputStream();
        System.setIn(inSupplier);
        System.setOut(new PrintStream(outRecorder));
        System.setErr(new PrintStream(errRecorder));
    }

    @BeforeEach
    public void reset() {
        inSupplier.reset();
        outRecorder.reset();
        errRecorder.reset();
    }

    @Test
    public void testInputModeNormal() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdInMode(InputMode.NORMAL)
                .build();
        var result = sandbox.run(Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.NORMAL, result.kind());
        assertEquals("Hello", result.value());
    }

    @Test
    public void testInputModeEmpty() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdInMode(EMPTY)
                .build();
        var result = sandbox.run(Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.NORMAL, result.kind());
        assertEquals("", result.value());
    }

    @Test
    public void testInputModeClosed() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdInMode(CLOSED)
                .build();
        var result = sandbox.run(Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.EXCEPTION, result.kind());
        assertEquals(IOException.class, result.exception().getClass());
    }

    @Test
    public void testOutputModeNormal() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdOutMode(NORMAL)
                .stdErrMode(NORMAL)
                .build();
        var result = sandbox.run(Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    @Test
    public void testOutputModeDiscard() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdOutMode(DISCARD)
                .stdErrMode(DISCARD)
                .build();
        var result = sandbox.run(Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecord() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdOutMode(RECORD)
                .stdErrMode(RECORD)
                .build();
        var result = sandbox.run(Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordTimeout() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofSeconds(1))
                .stdOutMode(RECORD)
                .stdErrMode(RECORD)
                .build();
        var result = sandbox.run(Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordForward() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdOutMode(RECORD_FORWARD)
                .stdErrMode(RECORD_FORWARD)
                .build();
        var result = sandbox.run(Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    public static class Input {
        public static String run() throws IOException {
            return new String(System.in.readNBytes(5));
        }
    }

    public static class Output {
        public static void run() {
            System.out.print("This goes out");
            System.err.print("This goes err");
        }
    }

    @Test
    public void testRestrictionsPermitted() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(Whitelisted.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.NORMAL, result.kind());
    }

    @Test
    public void testRestrictionsForbidden() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.ILLEGAL_OPERATION, result.kind());
        assertEquals(SecurityException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("java.util.Scanner(java.nio.file.Path)"));
    }

    @Test
    public void testRestrictionsTryCatchReturn() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(TryCatchReturn.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.ILLEGAL_OPERATION, result.kind(), result.exception().toString());
        assertEquals(SecurityException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("java.nio.file.Files.list"));
    }

    @Test
    public void testCustomRestrictions() throws IOException {
        var permitted = Whitelist.parse(Whitelist.DEFAULT_WHITELIST_DEF
                                        + "java.util.Scanner.<init>(java.nio.file.Path)");
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .permittedCalls(permitted)
                .build();
        var result = sandbox.run(IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.EXCEPTION, result.kind());
        assertEquals(NoSuchFileException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("test.txt"));
    }

    @Test
    public void testNoRestrictions() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .permittedCalls(null)
                .build();
        var result = sandbox.run(IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.EXCEPTION, result.kind());
        assertEquals(NoSuchFileException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("test.txt"));
    }

    public static class Whitelisted {
        public static void run() {
            // all of the following are permitted by the default whitelist,
            // including Path operations (that don't access the file system)
            // and the constructor of Scanner that uses a String as the source
            var path = Path.of("Hello/World");
            var scanner = new Scanner("Hello " + 2 + " " + path.getFileName());
            System.out.println(scanner.next());
            System.out.println(Math.abs(scanner.nextInt()));
            System.out.println(new Random().nextInt());
        }
    }

    public static class IO {
        public static void run() throws IOException {
            // this constructor is forbidden, as it allows reading from a file:
            var scanner = new Scanner(Path.of("test.txt"));
            System.out.println(scanner.next());
        }
    }

    public static class TryCatchReturn {
        public static int run() {
            // this construct leads to a weird "Illegal exception table" error
            // if not for the "if (true)" workaround in SandboxClassLoader
            try {
                return (int) Files.list(Path.of(".")).count(); // not permitted
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    @Test
    public void testTimeoutNormalLoop() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(NormalLoop.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutTightLoop() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(TightLoop.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutMultipleLoops() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(MultipleLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutNestedLoops() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(NestedLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutNestedTightLoops() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(NestedTightLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testCatchesInterruptedException() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(CatchesInterruptedException.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    public static class NormalLoop {
        public static void run() {
            int counter = 0;
            while (Math.random() > 0) {
                counter++;
            }
            System.out.println(counter);
        }
    }

    public static class TightLoop {
        public static void run() {
            while (true);
        }
    }

    public static class MultipleLoops {
        public static void run() {
            // this loop is fine
            for (int i = 0; i < 10; i++) {
                System.out.println(i);
            }
            // but not this one
            for (int i = 9; i >= 0; i--) {
                i++;
            }
        }
    }

    public static class NestedLoops {
        public static void run() {
            while (true) {
                System.out.println("outer");
                while (true) {
                    System.out.println("inner");
                }
            }
        }
    }

    public static class NestedTightLoops {
        public static void run() {
            while (true) {
                while (true);
            }
        }
    }

    public static class CatchesInterruptedException {
        public static void run() {
            while (true) {
                try {
                    if (Math.random() < Double.MIN_VALUE) {
                        throw new InterruptedException();
                    }
                    while (true);
                } catch (InterruptedException e) {
                    // happily ignore
                }
            }
        }
    }

    @Test
    void testInstrumentationInterface() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofSeconds(1))
                .build();
        var result = sandbox.run(Interface.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.NORMAL, result.kind(), () -> result.exception().toString());
    }

    public interface Interface {
        void foo(); // <- no code!

        static void run() {
            System.out.println("Hello, World!");
        }
    }

    private ClassPath code() {
        var url = SandboxTest.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return ClassPath.fromFiles(List.of(Path.of(url.toURI())));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
