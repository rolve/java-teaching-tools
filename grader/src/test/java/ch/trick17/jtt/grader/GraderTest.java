package ch.trick17.jtt.grader;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static ch.trick17.jtt.grader.Compiler.JAVAC;
import static java.nio.file.Files.list;
import static java.nio.file.Files.readAllLines;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class GraderTest {

    static final Path SUBM_ROOT = Path.of("test-submissions");
    static final Codebase ECLIPSE_BASE = new Codebase(
            SUBM_ROOT.resolve("eclipse-structure"), ProjectStructure.ECLIPSE);
    static final Codebase MVN_BASE = new Codebase(
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

    @Test
    public void testEclipseStructureEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest", ECLIPSE));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        // TODO: make results available through API
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    public void testEclipseStructureJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", JAVAC));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    public void testMavenStructureEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest", ECLIPSE));
        var grader = new Grader(MVN_BASE, tasks);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    public void testMavenStructureJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", JAVAC));
        var grader = new Grader(MVN_BASE, tasks);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    public void testPackageEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("multiply.MultiplyTest", ECLIPSE));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "1", "2");
        grader.run();
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
        var tasks = List.of(new Task("multiply.MultiplyTest", JAVAC));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "1", "2");
        grader.run();
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
        var tasks = List.of(new Task("AddTest", ECLIPSE));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "3");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "3\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", JAVAC));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "3");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "3\t0\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testCustomDir() throws IOException {
        var tasks = List.of(new Task("AddTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.setTestsDir(Path.of("tests-custom-dir"));
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd",
                "0\t1\t0\t1",
                "1\t1\t0\t0",
                "2\t1\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testSingleDeduction() throws IOException {
        var tasks = List.of(new Task("AddTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "4");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tchanged signature\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "4\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testMultipleDeductions() throws IOException {
        var tasks = List.of(new Task("DivideTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "4");
        grader.run();
        var results = readAllLines(Path.of("results-DivideTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tchanged signature\twrong package\ttestDivide",
                "0\t1\t0\t0\t1",
                "4\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testDeductionsPackage() throws IOException {
        var tasks = List.of(new Task("multiply.MultiplyTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "4");
        grader.run();
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tchanged signature\ttestMultiply1\ttestMultiply2",
                "0\t1\t0\t1\t1",
                "4\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testTimeout() throws IOException {
        var tasks = List.of(new Task("AddTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "5"); // contains infinite loop
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttimeout\tincomplete repetitions\ttestAdd1\ttestAdd2",
                "0\t1\t0\t0\t1\t1",
                "5\t1\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingClassUnderTestEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "6");
        grader.run();
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
        var tasks = List.of(new Task("AddTest", JAVAC));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "6");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "6\t0\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingSrcDir() throws IOException {
        var tasks = List.of(new Task("AddTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "7");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t1\t0\t1\t1",
                "7\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNondeterminism() throws IOException {
        var tasks = List.of(new Task("AddTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "8");
        grader.run();
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
        var tasks = List.of(new Task("SubtractTest"));
        var grader = new Grader(ECLIPSE_BASE, tasks);
        grader.gradeOnly("0", "9");
        grader.run();
        var results = readAllLines(Path.of("results-SubtractTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tnondeterministic\ttimeout\tincomplete repetitions\ttestSubtract1\ttestSubtract2\ttestSubtract3\ttestSubtract4\ttestSubtract5\ttestSubtract6",
                "0\t1\t0\t0\t0\t1\t1\t1\t1\t1\t1",
                "9\t1\t1\t1\t1\t0\t0\t0\t0\t0\t0");
        assertEquals(expected, results);
    }

    @AfterAll
    public static void deleteLogFiles() throws IOException {
        try (var allFiles = list(Path.of("."))) {
            allFiles.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .matches("grader_.*\\.log|results-.*\\.tsv"))
                    .forEach(p -> p.toFile().delete());
        }
    }
}
