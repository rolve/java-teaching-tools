package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.memcompile.InMemCompilation;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestRunner.Task;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class TestRunnerTest {

    static final String SIMPLE_TEST = """
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
        var test = compile(SIMPLE_TEST);

        var result = runner.run(new Task("PassingTest",
                ClassPath.empty(), ClassPath.fromCurrent().withMemory(test)));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());

        result = runner.run(new Task("FailingTest",
                ClassPath.empty(), ClassPath.fromCurrent().withMemory(test)));
        assertEquals(1, result.testResults().size());
        assertFalse(result.testResults().get(0).passed());
    }

    @Test
    void testAsSandboxedCode() throws IOException {
        var test = compile(SIMPLE_TEST);
        var result = runner.run(new Task("PassingTest",
                ClassPath.fromMemory(test), ClassPath.fromCurrent()));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());

        result = runner.run(new Task("FailingTest",
                ClassPath.fromMemory(test), ClassPath.fromCurrent()));
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

    private static List<InMemClassFile> compile(String tests) throws IOException {
        return InMemCompilation.compile(JAVAC,
                List.of(InMemSource.fromString(tests)),
                ClassPath.fromCurrent(), System.out).output();
    }
}
