package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemCompilation;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestRunner.Task;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static org.junit.jupiter.api.Assertions.*;

public class TestRunnerTest {

    TestRunner runner = new TestRunner();

    @Test
    void testAsSupportCode() throws IOException {
        var testClass = """
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
        var compiled = InMemCompilation.compile(JAVAC, List.of(InMemSource.fromString(testClass)),
                ClassPath.fromCurrent(), System.out).output();

        var result = runner.run(new Task("PassingTest",
                ClassPath.empty(), ClassPath.fromCurrent().withMemory(compiled)));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());

        result = runner.run(new Task("FailingTest",
                ClassPath.empty(), ClassPath.fromCurrent().withMemory(compiled)));
        assertEquals(1, result.testResults().size());
        assertFalse(result.testResults().get(0).passed());
    }

    @Test
    void testAsSandboxedCode() throws IOException {
        var testClass = """
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
        var compiled = InMemCompilation.compile(JAVAC, List.of(InMemSource.fromString(testClass)),
                ClassPath.fromCurrent(), System.out).output();

        var result = runner.run(new Task("PassingTest",
                ClassPath.fromMemory(compiled), ClassPath.fromCurrent()));
        assertEquals(1, result.testResults().size());
        assertTrue(result.testResults().get(0).passed());

        result = runner.run(new Task("FailingTest",
                ClassPath.fromMemory(compiled), ClassPath.fromCurrent()));
        assertEquals(1, result.testResults().size());
        assertFalse(result.testResults().get(0).passed());
    }
}
