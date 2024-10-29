package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.sandbox.Sandbox.Result.Kind;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static ch.trick17.jtt.memcompile.InMemCompilation.compile;
import static ch.trick17.jtt.sandbox.InputMode.CLOSED;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.*;
import static ch.trick17.jtt.sandbox.Sandbox.Result.Kind.EXCEPTION;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class SandboxTest {

    // Many features are tested indirectly in GraderTest, not here

    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;
    private static ByteArrayInputStream inSupplier;
    private static ByteArrayOutputStream outRecorder;
    private static ByteArrayOutputStream errRecorder;

    @BeforeAll
    static void install() {
        inSupplier = new ByteArrayInputStream("Hello, World!".getBytes());
        outRecorder = new ByteArrayOutputStream();
        errRecorder = new ByteArrayOutputStream();
        System.setIn(inSupplier);
        System.setOut(new PrintStream(outRecorder));
        System.setErr(new PrintStream(errRecorder));
    }

    @BeforeEach
    void reset() {
        inSupplier.reset();
        outRecorder.reset();
        errRecorder.reset();
    }

    @Test
    void isolation() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(WithStaticFields.class, "hellos",
                emptyList(), emptyList(), List.class);
        var expected = List.of(
                """
                Hello, 1!
                """,
                """
                Hello, 1!
                Hello, 2!
                """,
                """
                Hello, 1!
                Hello, 2!
                Hello, 3!
                """);
        assertEquals(expected, result.value());

        // when running again, static fields should be reset:
        result = sandbox.run(WithStaticFields.class, "hellos",
                emptyList(), emptyList(), List.class);
        assertEquals(expected, result.value());
    }

    @Test
    void isolationSomeFieldsNotInitialized() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(WithUninitializedStaticField.class, "increment",
                emptyList(), emptyList(), Integer.class);
        assertEquals(0, result.value());

        result = sandbox.run(WithUninitializedStaticField.class, "increment",
                emptyList(), emptyList(), Integer.class);
        assertEquals(0, result.value());

        result = sandbox.run(WithUninitializedStaticField.class, "increment",
                emptyList(), emptyList(), Integer.class);
        assertEquals(0, result.value());
    }

    @Test
    void isolationFinalStaticField() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(WithFinalStaticField.class, "increment",
                emptyList(), emptyList(), Integer.class);
        assertEquals(0, result.value());

        result = sandbox.run(WithFinalStaticField.class, "increment",
                emptyList(), emptyList(), Integer.class);
        assertEquals(0, result.value());

        result = sandbox.run(WithFinalStaticField.class, "increment",
                emptyList(), emptyList(), Integer.class);
        assertEquals(0, result.value());
    }

    @Test
    void isolationEnum() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(Status.class, "test",
                emptyList(), emptyList(), Boolean.class);
        assertEquals(true, result.value());

        result = sandbox.run(Status.class, "test",
                emptyList(), emptyList(), Boolean.class);
        assertEquals(true, result.value());
    }

    public static class WithStaticFields {
        private static int count = 0;
        private static String s = "";

        public static String hello() {
            count++;
            var line = "Hello, " + count + "!\n";
            s += line;
            return s;
        }

        public static List<String> hellos() {
            var list = new ArrayList<String>();
            for (int i = 0; i < 3; i++) {
                list.add(hello());
            }
            return list;
        }
    }

    public static class WithUninitializedStaticField {
        private static int count;

        public static int increment() {
            return count++;
        }
    }

    public static class WithFinalStaticField {
        private static final int[] count = {0};

        public static int increment() {
            return count[0]++;
        }
    }

    public enum Status {
        OK, ERROR, UNKNOWN;

        public boolean isKnown() {
            return this == OK || this == ERROR;
        }

        public static boolean test() {
            return valueOf("OK").isKnown();
        }
    }

    @Test
    void inputModeNormal() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdInMode(InputMode.NORMAL)
                .build();
        var result = sandbox.run(Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals("Hello", result.value());
    }

    @Test
    void inputModeEmpty() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdInMode(EMPTY)
                .build();
        var result = sandbox.run(Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals("", result.value());
    }

    @Test
    void inputModeClosed() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .stdInMode(CLOSED)
                .build();
        var result = sandbox.run(Input.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(EXCEPTION, result.kind());
        assertEquals(IOException.class, result.exception().getClass());
    }

    @Test
    void outputModeNormal() throws IOException {
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
    void outputModeDiscard() throws IOException {
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
    void outputModeRecord() throws IOException {
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
    void outputModeRecordTimeout() throws IOException {
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
    void outputModeRecordForward() throws IOException {
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
    void restrictionsPermitted() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(Whitelisted.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.NORMAL, result.kind());
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

    @Test
    void restrictionsForbidden() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.ILLEGAL_OPERATION, result.kind());
        assertEquals(SecurityException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("java.util.Scanner(java.nio.file.Path)"));
    }

    public static class IO {
        public static void run() throws IOException {
            // this constructor is forbidden, as it allows reading from a file:
            var scanner = new Scanner(Path.of("test.txt"));
            System.out.println(scanner.next());
        }
    }

    @Test
    void restrictionsTryCatchReturn() throws IOException {
        var sandbox = new Sandbox(code(), ClassPath.empty());
        var result = sandbox.run(TryCatchReturn.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.ILLEGAL_OPERATION, result.kind(), result.exception().toString());
        assertEquals(SecurityException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("java.nio.file.Files.list"));
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
    void customRestrictions() throws IOException {
        var permitted = Whitelist.parse(Whitelist.DEFAULT_WHITELIST_DEF
                                        + "java.util.Scanner.<init>(java.nio.file.Path)");
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .permittedCalls(permitted)
                .build();
        var result = sandbox.run(IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(EXCEPTION, result.kind());
        assertEquals(NoSuchFileException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("test.txt"));
    }

    @Test
    void noRestrictions() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .permittedCalls(null)
                .build();
        var result = sandbox.run(IO.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(EXCEPTION, result.kind());
        assertEquals(NoSuchFileException.class, result.exception().getClass());
        assertTrue(result.exception().getMessage().contains("test.txt"));
    }

    @Test
    void restrictionsAssertKeyword() throws IOException {
        // needs to be executed with assertions enabled (-ea)
        var compiled = compile(ECLIPSE, List.of(InMemSource.fromString("""
                public class Asserter {
                    public static void assertSomething() {
                        System.out.println(Asserter.class.desiredAssertionStatus());
                        assert false : "oops";
                    }
                }
                """)), ClassPath.empty(), System.out).output();

        var sandbox = new Sandbox.Builder(ClassPath.fromMemory(compiled), ClassPath.empty())
                .timeout(Duration.ofSeconds(1))
                .stdOutMode(RECORD)
                .build();
        var result = sandbox.run("Asserter", "assertSomething",
                emptyList(), emptyList(), void.class);
        originalOut.println(result.stdOut());
        assertEquals(EXCEPTION, result.kind());
        assertInstanceOf(AssertionError.class, result.exception());
        assertEquals("oops", result.exception().getMessage());
    }

    @Test
    void timeoutNormalLoop() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(NormalLoop.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    void timeoutTightLoop() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(TightLoop.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    void timeoutMultipleLoops() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(MultipleLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    void timeoutNestedLoops() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(NestedLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    void timeoutNestedTightLoops() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run(NestedTightLoops.class, "run",
                emptyList(), emptyList(), Void.class);
        assertEquals(Kind.TIMEOUT, result.kind());
    }

    @Test
    void catchesInterruptedException() throws IOException {
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
    void instrumentationInterface() throws IOException {
        var sandbox = new Sandbox.Builder(code(), ClassPath.empty())
                .timeout(Duration.ofSeconds(1))
                .build();
        var result = sandbox.run(Interface.class, "hello",
                emptyList(), emptyList(), String.class);
        assertEquals("Hello, World!", result.value());
    }

    public interface Interface {
        void foo(); // <- no code!

        static String hello() {
            return "Hello, World!";
        }
    }

    @Test
    void instrumentationStackHeight() throws IOException {
        // this code used to cause a BadBytecode error because the stack height
        // was not properly increased (only with the Eclipse compiler)
        var compiled = compile(ECLIPSE, List.of(InMemSource.fromString("""
                import java.util.List;
                public class StackHeight {
                    public static void run() {
                        var numbers = List.of();
                        for (var n : numbers) {
                            if (numbers.contains(n)) {
                                System.out.println(n);
                            }
                        }
                    }
                }
                """)), ClassPath.empty(), System.out).output();

        var sandbox = new Sandbox.Builder(ClassPath.fromMemory(compiled),
                ClassPath.empty()) .timeout(Duration.ofMillis(500))
                .build();
        var result = sandbox.run("StackHeight", "run",
                emptyList(), emptyList(), List.class);
        assertEquals(Kind.NORMAL, result.kind(), () -> result.exception().toString());
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
