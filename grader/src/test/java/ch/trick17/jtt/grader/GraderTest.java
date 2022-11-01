package ch.trick17.jtt.grader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static ch.trick17.jtt.grader.Compiler.JAVAC;
import static java.nio.file.Files.list;
import static java.nio.file.Files.readAllLines;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class GraderTest {

    static final Path SUBM_ROOT = Path.of("test-submissions");
    static final Codebase ECLIPSE_BASE = new MultiCodebase(
            SUBM_ROOT.resolve("eclipse-structure"), ProjectStructure.ECLIPSE);
    static final Codebase MVN_BASE = new MultiCodebase(
            SUBM_ROOT.resolve("maven-structure"), ProjectStructure.MAVEN);

    static final List<String> EXPECTED_ADD_SIMPLE_EC = List.of(
            "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
            "0\t1\t0\t1\t1",
            "1\t1\t0\t1\t0",
            "2\t1\t1\t0\t0");
    static final List<String> EXPECTED_ADD_SIMPLE_JC = List.of(
            "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
            "0\t1\t0\t1\t1",
            "1\t1\t0\t1\t0",
            "2\t0\t1\t0\t0");

    private static Grader grader;

    @BeforeAll
    public static void setUp() {
        grader = new Grader();
        grader.setLogDir(null);
    }

    @Test
    public void testResults() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(JAVAC));
        grader.gradeOnly("0", "1", "2");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(3, results.submissionResults().size());

        assertTrue(results.get("0").testResults().methodResults().get(0).passed());
        assertTrue(results.get("0").testResults().methodResults().get(1).passed());

        assertTrue(results.get("1").testResults().methodResults().get(0).passed());
        assertFalse(results.get("1").testResults().methodResults().get(1).passed());

        assertTrue(results.get("2").compileErrors());
        assertFalse(results.get("2").compiled());
        assertNull(results.get("2").testResults());
    }

    @Test
    public void testEclipseStructureEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(ECLIPSE));
        grader.gradeOnly("0", "1", "2");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    public void testEclipseStructureJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(JAVAC));
        grader.gradeOnly("0", "1", "2");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    public void testMavenStructureEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(ECLIPSE));
        grader.gradeOnly("0", "1", "2");
        grader.run(MVN_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    public void testMavenStructureJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(JAVAC));
        grader.gradeOnly("0", "1", "2");
        grader.run(MVN_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    public void testPackageEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("multiply.MultiplyTest").compiler(ECLIPSE));
        grader.gradeOnly("0", "1", "2");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestMultiply1\ttestMultiply2",
                "0\t1\t0\t1\t1",
                "1\t1\t0\t1\t0",
                "2\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testPackageJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("multiply.MultiplyTest").compiler(JAVAC));
        grader.gradeOnly("0", "1", "2");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestMultiply1\ttestMultiply2",
                "0\t1\t0\t1\t1",
                "1\t1\t0\t1\t0",
                "2\t0\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(ECLIPSE));
        grader.gradeOnly("0", "3");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "3\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(JAVAC));
        grader.gradeOnly("0", "3");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "3\t0\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testCustomDir() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", Path.of("tests-custom-dir")));
        grader.gradeOnly("0", "1", "2");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd",
                "0\t1\t0\t1",
                "1\t1\t0\t0",
                "2\t1\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    @Disabled // TODO
    public void testSingleDeduction() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest"));
        grader.gradeOnly("0", "4");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tchanged signature\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "4\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    @Disabled // TODO
    public void testMultipleDeductions() throws IOException {
        var tasks = List.of(Task.fromClassName("DivideTest"));
        grader.gradeOnly("0", "4");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-DivideTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tchanged signature\twrong package\ttestDivide",
                "0\t1\t0\t0\t1",
                "4\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    @Disabled // TODO
    public void testDeductionsPackage() throws IOException {
        var tasks = List.of(Task.fromClassName("multiply.MultiplyTest"));
        grader.gradeOnly("0", "4");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tchanged signature\ttestMultiply1\ttestMultiply2",
                "0\t1\t0\t1\t1",
                "4\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testTimeout() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").repetitions(3));
        grader.gradeOnly("0", "5"); // contains infinite loop
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttimeout\tincomplete repetitions\ttestAdd1\ttestAdd2",
                "0\t1\t0\t0\t1\t1",
                "5\t1\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingClassUnderTestEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest"));
        grader.gradeOnly("0", "6");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        // TODO: would be nice to have an entry for this
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "6\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingClassUnderTestJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").compiler(JAVAC));
        grader.gradeOnly("0", "6");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "6\t0\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingSrcDir() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest"));
        grader.gradeOnly("0", "7");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "7\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNondeterminism() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest")
                .repetitions(50)
                .timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60)));
        grader.gradeOnly("0", "8");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tnondeterministic\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "8\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNondeterminismPlusTimeout() throws IOException {
        // ensure that the timeout of one test does not affect detection
        // of nondeterminism in other tests, as was previously the case
        var tasks = List.of(Task.fromClassName("SubtractTest")
                .repetitions(5)
                .permRestrictions(false)); // needs access to system properties
        grader.gradeOnly("0", "9");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-SubtractTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tnondeterministic\ttimeout\tincomplete repetitions\ttestSubtract1\ttestSubtract2\ttestSubtract3\ttestSubtract4\ttestSubtract5\ttestSubtract6",
                "0\t1\t0\t0\t0\t1\t1\t1\t1\t1\t1",
                "9\t1\t1\t1\t1\t0\t0\t0\t0\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testIsolation() throws IOException {
        // ensure that classes are reloaded for each test run, meaning
        // that tests cannot interfere via static fields
        var tasks = List.of(Task.fromClassName("SubtractTest"));
        grader.gradeOnly("0", "10");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-SubtractTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttestSubtract1\ttestSubtract2\ttestSubtract3\ttestSubtract4\ttestSubtract5\ttestSubtract6",
                "0\t1\t1\t1\t1\t1\t1\t1",
                "10\t1\t1\t1\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testCatchThreadDeath() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").repetitions(3));
        grader.gradeOnly("0", "11"); // contains infinite loop plus catch(ThreadDeath)
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttimeout\tincomplete repetitions\ttestAdd1\ttestAdd2",
                "0\t1\t0\t0\t1\t1",
                "11\t1\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testSystemIn() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest").repetitions(3));
        grader.gradeOnly("0", "12");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttestAdd1\ttestAdd2",
                "0\t1\t1\t1",
                "12\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testIllegalOperationIO() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest"));
        grader.gradeOnly("0", "13"); // tries to read from the file system
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tillegal operation\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "13\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testIllegalOperationReflection() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest"));
        grader.gradeOnly("0", "14"); // uses getDeclaredMethods()
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tillegal operation\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "14\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNoTests() throws IOException {
        var tasks = List.of(Task.fromClassName("NoTests").compiler(ECLIPSE));
        grader.gradeOnly("0");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);
        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(1, results.submissionResults().size());
        var submResults = results.submissionResults().get(0);
        assertEquals(emptyList(), submResults.passedTests());
        assertEquals(emptyList(), submResults.failedTests());
    }

    @Test
    public void testNestedTestClasses() throws IOException {
        var tasks = List.of(Task.fromClassName("NestedTestClass").compiler(ECLIPSE));
        grader.gradeOnly("0", "2");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(2, results.submissionResults().size());

        var allTests = List.of("MultiplyTest.testMultiply1", "MultiplyTest.testMultiply2"); // note alphabetical order
        assertEquals(allTests, results.get("0").passedTests());
        assertEquals(emptyList(), results.get("0").failedTests());
        assertEquals(emptyList(), results.get("2").passedTests());
        assertEquals(allTests, results.get("2").failedTests());
    }

    @Test
    public void testAssumptions() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithAssumption").compiler(ECLIPSE));
        grader.gradeOnly("0");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(1, results.submissionResults().size());

        var testResults = results.get("0").testResults();
        assertTrue(testResults.methodResultFor("testNormal").get().passed());
        assertFalse(testResults.methodResultFor("testFailedAssumption").get().passed());
    }

    @Test
    public void testDisabled() throws IOException {
        var tasks = List.of(Task.fromClassName("DisabledTest").compiler(ECLIPSE));
        grader.gradeOnly("0");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(1, results.submissionResults().size());

        var testResults = results.get("0").testResults();
        assertTrue(testResults.methodResultFor("testNormal").get().passed());
        assertTrue(testResults.methodResultFor("testDisabled").get().passed());

        // TODO: better support for disabled tests
    }

    @Test
    public void testDefaultMethodOrder() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithDisplayNames").compiler(ECLIPSE));
        grader.gradeOnly("0");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-TestWithDisplayNames.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttest3\ttest2\ttest4\ttest1",
                "0\t1\t1\t0\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testCustomMethodOrder() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithMethodOrder").compiler(ECLIPSE));
        grader.gradeOnly("0");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-TestWithMethodOrder.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttest4\ttest3\ttest2\ttest1",
                "0\t1\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        grader.close();
        try (var allFiles = list(Path.of("."))) {
            allFiles.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .matches("results-.*\\.tsv"))
                    .forEach(p -> p.toFile().delete());
        }
    }
}
