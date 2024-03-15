package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.Grader.Task;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class GraderTest {

    static final Path TEST_SRC_DIR = Path.of("tests");
    static final Path SUBM_ROOT = Path.of("test-submissions");

    static List<InMemSource> correct;
    static List<InMemSource> failsTest;
    static List<InMemSource> compileError;
    static Grader grader;

    @BeforeAll
    static void setup() throws IOException {
        correct = InMemSource.fromDirectory(
                SUBM_ROOT.resolve("eclipse-structure/correct/src"), null);
        failsTest = InMemSource.fromDirectory(
                SUBM_ROOT.resolve("eclipse-structure/fails-test/src"), null);
        compileError = InMemSource.fromDirectory(
                SUBM_ROOT.resolve("eclipse-structure/compile-error/src"), null);
        grader = new Grader();
    }

    @Test
    void results() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC);

        var result = grader.grade(task, correct);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertTrue(result.testResults().get(0).passed());
        assertTrue(result.testResults().get(1).passed());

        result = grader.grade(task, failsTest);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertTrue(result.testResults().get(0).passed());
        assertFalse(result.testResults().get(1).passed());

        result = grader.grade(task, compileError);
        assertFalse(result.compiled());
        assertTrue(result.compileErrors());
        assertNull(result.testResults());
    }

    @Test
    void noTests() throws IOException {
        var task = Task.fromClassName("NoTests", TEST_SRC_DIR).compiler(ECLIPSE);
        var result = grader.grade(task, correct);
        assertEquals(emptyList(), result.passedTests());
        assertEquals(emptyList(), result.failedTests());
    }

    @Test
    void nestedTestClasses() throws IOException {
        var task = Task.fromClassName("NestedTestClass", TEST_SRC_DIR).compiler(ECLIPSE);
        var allTests = List.of(
                new TestMethod("NestedTestClass.MultiplyTest", "multiply1"),
                new TestMethod("NestedTestClass.MultiplyTest", "multiply2")); // note alphabetical order

        var result = grader.grade(task, correct);
        assertEquals(allTests, result.passedTests());
        assertEquals(emptyList(), result.failedTests());

        result = grader.grade(task, compileError);
        assertEquals(emptyList(), result.passedTests());
        assertEquals(allTests, result.failedTests());
    }

    @Test
    void assumptions() throws IOException {
        var task = Task.fromClassName("TestWithAssumption", TEST_SRC_DIR).compiler(ECLIPSE);
        var result = grader.grade(task, correct);
        assertTrue(result.testResultFor("normal").passed());
        assertFalse(result.testResultFor("failedAssumption").passed());
    }

    @Test
    void disabled() throws IOException {
        var task = Task.fromClassName("DisabledTest", TEST_SRC_DIR).compiler(ECLIPSE);
        var result = grader.grade(task, correct);
        assertTrue(result.testResultFor("normal").passed());
        assertTrue(result.testResultFor("disabled").passed());

        // TODO: better support for disabled tests
    }

    @Test
    void score() throws IOException {
        var task = Task.fromClassName("TestWithScore", TEST_SRC_DIR)
                .compiler(ECLIPSE)
                .repetitions(20);

        // "correct" passes all tests
        var result = grader.grade(task, correct);
        assertTrue(result.testResultFor("withScore").passed());
        var scores = result.testResultFor("withScore").scores();
        assertEquals(20, scores.size());
        assertEquals(Set.of(50.0, 100.0), Set.copyOf(scores));

        assertTrue(result.testResultFor("withScoreFirst").passed());
        scores = result.testResultFor("withScoreFirst").scores();
        assertEquals(20, scores.size());
        assertEquals(Set.of(100.0), Set.copyOf(scores));

        assertTrue(result.testResultFor("withoutScore").passed());
        assertTrue(result.testResultFor("withoutScore").scores().isEmpty());

        assertTrue(result.testResultFor("withOrWithoutScore").passed());
        scores = result.testResultFor("withOrWithoutScore").scores();
        assertTrue(!scores.isEmpty() && scores.size() < 20);
        assertEquals(Set.of(100.0), Set.copyOf(scores));

        // "fails-test" fails all tests
        result = grader.grade(task, failsTest);
        assertFalse(result.testResultFor("withScore").passed());
        assertTrue(result.testResultFor("withScore").scores().isEmpty());

        assertFalse(result.testResultFor("withScoreFirst").passed());
        scores = result.testResultFor("withScoreFirst").scores();
        assertEquals(20, scores.size());
        assertEquals(Set.of(100.0), Set.copyOf(scores));

        assertFalse(result.testResultFor("withoutScore").passed());
        assertEquals(0, result.testResultFor("withoutScore").scores().size());

        assertFalse(result.testResultFor("withOrWithoutScore").passed());
        assertTrue(result.testResultFor("withOrWithoutScore").scores().isEmpty());
    }

    @Test
    void params() throws IOException {
        var task = Task.fromClassName("TestWithParams", TEST_SRC_DIR).compiler(ECLIPSE);
        var result = grader.grade(task, correct);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertFalse(result.testCompileErrors());
        assertTrue(result.testResultFor("add(int)").passed());

        result = grader.grade(task, failsTest);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertFalse(result.testCompileErrors());
        assertFalse(result.testResultFor("add(int)").passed());
    }

    @Test
    void testMethodOverloading() throws IOException {
        var task = Task.fromClassName("TestWithOverloading", TEST_SRC_DIR).compiler(ECLIPSE);
        var result = grader.grade(task, correct);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertFalse(result.testCompileErrors());
        assertEquals(2, result.testResults().size());
        assertTrue(result.testResultFor("add").passed());
        assertTrue(result.testResultFor("add(int)").passed());

        result = grader.grade(task, failsTest);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertFalse(result.testCompileErrors());
        assertEquals(2, result.testResults().size());
        assertFalse(result.testResultFor("add").passed());
        assertFalse(result.testResultFor("add(int)").passed());
    }

    @Test
    void paramsMethodSource() throws IOException {
        var task = Task.fromClassName("TestWithMethodSource", TEST_SRC_DIR).compiler(ECLIPSE);
        var result = grader.grade(task, correct);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertFalse(result.testCompileErrors());
        assertTrue(result.testResultFor("add(int, int)").passed());

        result = grader.grade(task, failsTest);
        assertTrue(result.compiled());
        assertFalse(result.compileErrors());
        assertFalse(result.testCompileErrors());
        assertFalse(result.testResultFor("add(int, int)").passed());
    }
}
