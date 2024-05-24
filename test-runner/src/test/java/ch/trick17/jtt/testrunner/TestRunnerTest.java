package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.memcompile.InMemCompilation;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestRunner.Result;
import ch.trick17.jtt.testrunner.TestRunner.Task;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class TestRunnerTest {

    static final String SIMPLE_TESTS = """
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            
            class PassingTest {
                @Test
                void test() {
                    assertEquals(2, 1 + 1);
                }
            }
            class FailingTest {
                @Test
                void test() {
                    assertEquals(3, 1 + 1);
                }
            }
            """;

    TestRunner runner = new TestRunner();

    @Test
    void testAsSupportCode() throws IOException {
        var tests = compile(SIMPLE_TESTS);

        var result = runner.run(new Task("PassingTest",
                ClassPath.empty(), ClassPath.fromCurrent().withMemory(tests)));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());

        result = runner.run(new Task("FailingTest",
                ClassPath.empty(), ClassPath.fromCurrent().withMemory(tests)));
        assertEquals(1, result.testResults().size());
        assertFalse(result.testResults().get(0).passed());
    }

    @Test
    void testAsSandboxedCode() throws IOException {
        var tests = compile(SIMPLE_TESTS);
        var result = runner.run(new Task("PassingTest",
                ClassPath.fromMemory(tests), ClassPath.fromCurrent()));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());

        result = runner.run(new Task("FailingTest",
                ClassPath.fromMemory(tests), ClassPath.fromCurrent()));
        assertEquals(1, result.testResults().size());
        assertFalse(result.testResults().get(0).passed());
    }

    @Test
    void nonPublicClassWithReInit() throws IOException {
        var test = compile("""
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                
                class SomeTest {
                    @Test
                    void test() {
                        assertEquals(0, SomeClass.incrementAndGet());
                        assertEquals(1, SomeClass.incrementAndGet());
                        assertEquals(2, SomeClass.incrementAndGet());
                    }
                }
                
                class SomeClass {
                    private static int i = 0;
                    public static int incrementAndGet() {
                        return i++;
                    }
                }
                """);

        var result = runner.run(new Task(List.of("SomeTest"),
                ClassPath.fromMemory(test), ClassPath.fromCurrent(), 3,
                Duration.ofSeconds(1), Duration.ofSeconds(1), null, emptyList()));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());
    }

    @Test
    void multithreading() throws IOException, InterruptedException, ExecutionException {
        var tests = compile(SIMPLE_TESTS);

        var executor = Executors.newFixedThreadPool(10);
        var results = new ArrayList<Future<Result>>();
        for (int i = 1; i <= 10; i++) {
            results.add(executor.submit(() -> runner.run(new Task(List.of("PassingTest"),
                    ClassPath.empty(), ClassPath.fromCurrent().withMemory(tests),
                    5, Duration.ofSeconds(5), Duration.ofSeconds(10), null, emptyList()))));
        }
        for (var result : results) {
            assertEquals(1, result.get().testResults().size());
            assertTrue(result.get().testResults().get(0).passed());
        }
    }

    @Test
    void multithreadingVmArgs() throws IOException, InterruptedException, ExecutionException {
        var tests = compile(SIMPLE_TESTS);
        var executor = Executors.newFixedThreadPool(50);
        var results = new ArrayList<Future<Result>>();
        for (int i = 1; i <= 50; i++) {
            // every 5th test uses different VM args, so a new VM is started
            var encoding = i % 5 == 0 ? "ISO-8859-1" : "UTF8";
            results.add(executor.submit(() -> runner.run(new Task(List.of("PassingTest"),
                    ClassPath.empty(), ClassPath.fromCurrent().withMemory(tests),
                    5, Duration.ofSeconds(10), Duration.ofSeconds(10), null,
                    List.of("-Dfile.encoding=" + encoding)))));
        }
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            assertEquals(1, result.get().testResults().size());
            assertTrue(result.get().testResults().get(0).passed(), result.get().toString());
        }
    }

    private static List<InMemClassFile> compile(String tests) throws IOException {
        return InMemCompilation.compile(JAVAC,
                List.of(InMemSource.fromString(tests)),
                ClassPath.fromCurrent(), System.out).output();
    }
}
