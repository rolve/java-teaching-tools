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
    public void testInputModeNormal() {
        var sandbox = new Sandbox().stdInMode(InputMode.NORMAL);
        var result = sandbox.run(code(), ClassPath.empty(), Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.NORMAL, result.kind());
        assertEquals("Hello", result.value());
    }

    @Test
    public void testInputModeEmpty() {
        var sandbox = new Sandbox().stdInMode(EMPTY);
        var result = sandbox.run(code(), ClassPath.empty(), Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.NORMAL, result.kind());
        assertEquals("", result.value());
    }

    @Test
    public void testInputModeClosed() {
        var sandbox = new Sandbox().stdInMode(CLOSED);
        var result = sandbox.run(code(), ClassPath.empty(), Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.EXCEPTION, result.kind());
        assertEquals(IOException.class, result.exception().getClass());
    }

    @Test
    public void testOutputModeNormal() {
        var sandbox = new Sandbox()
                .stdOutMode(NORMAL)
                .stdErrMode(NORMAL);
        var result = sandbox.run(code(), ClassPath.empty(), Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    @Test
    public void testOutputModeDiscard() {
        var sandbox = new Sandbox()
                .stdOutMode(DISCARD)
                .stdErrMode(DISCARD);
        var result = sandbox.run(code(), ClassPath.empty(), Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecord() {
        var sandbox = new Sandbox()
                .stdOutMode(RECORD)
                .stdErrMode(RECORD);
        var result = sandbox.run(code(), ClassPath.empty(), Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordTimeout() {
        var sandbox = new Sandbox()
                .timeout(Duration.ofSeconds(1))
                .stdOutMode(RECORD)
                .stdErrMode(RECORD);
        var result = sandbox.run(code(), ClassPath.empty(), Output.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordForward() {
        var sandbox = new Sandbox()
                .stdOutMode(RECORD_FORWARD)
                .stdErrMode(RECORD_FORWARD);
        var result = sandbox.run(code(), ClassPath.empty(), Output.class, "run",
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
    public void testRestrictionsPermitted() {
        var sandbox = new Sandbox();
        var result = sandbox.run(code(), ClassPath.empty(), Whitelisted.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.NORMAL, result.kind());
    }

    @Test
    public void testRestrictionsForbidden() {
        var sandbox = new Sandbox();
        var result = sandbox.run(code(), ClassPath.empty(), IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.ILLEGAL_OPERATION, result.kind());
        assertEquals(SecurityException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("java.util.Scanner(java.nio.file.Path)"));
    }

    @Test
    public void testRestrictionsTryCatchReturn() {
        var sandbox = new Sandbox();
        var result = sandbox.run(code(), ClassPath.empty(), TryCatchReturn.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.ILLEGAL_OPERATION, result.kind(), result.exception().toString());
        assertEquals(SecurityException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("java.nio.file.Files.list"));
    }

    @Test
    public void testCustomRestrictions() {
        var permitted = Whitelist.parse(Whitelist.DEFAULT_WHITELIST_DEF
                                        + "java.util.Scanner.<init>(java.nio.file.Path)");
        var sandbox = new Sandbox().permittedCalls(permitted);
        var result = sandbox.run(code(), ClassPath.empty(), IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.EXCEPTION, result.kind());
        assertEquals(NoSuchFileException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("test.txt"));
    }

    @Test
    public void testNoRestrictions() {
        var sandbox = new Sandbox().permittedCalls(null);
        var result = sandbox.run(code(), ClassPath.empty(), IO.class, "run",
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
    public void testTimeoutNormalLoop() {
        var sandbox = new Sandbox().timeout(Duration.ofMillis(500));
        var result = sandbox.run(code(), ClassPath.empty(), NormalLoop.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutTightLoop() {
        var sandbox = new Sandbox().timeout(Duration.ofMillis(500));
        var result = sandbox.run(code(), ClassPath.empty(), TightLoop.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutMultipleLoops() {
        var sandbox = new Sandbox().timeout(Duration.ofMillis(500));
        var result = sandbox.run(code(), ClassPath.empty(), MultipleLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutNestedLoops() {
        var sandbox = new Sandbox().timeout(Duration.ofMillis(500));
        var result = sandbox.run(code(), ClassPath.empty(), NestedLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testTimeoutNestedTightLoops() {
        var sandbox = new Sandbox().timeout(Duration.ofMillis(500));
        var result = sandbox.run(code(), ClassPath.empty(), NestedTightLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    public void testCatchesInterruptedException() {
        var sandbox = new Sandbox().timeout(Duration.ofMillis(500));
        var result = sandbox.run(code(), ClassPath.empty(), CatchesInterruptedException.class, "run",
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

    private ClassPath code() {
        var url = SandboxTest.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return ClassPath.fromFiles(List.of(Path.of(url.toURI())));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
