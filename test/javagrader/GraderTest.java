package javagrader;

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
    static final Path ECLIPSE_ROOT = SUBM_ROOT.resolve("eclipse-structure");
    static final Path MVN_ROOT = SUBM_ROOT.resolve("maven-structure");

    static final List<String> EXPECTED_ADD_SIMPLE = List.of(
            "Name\tcompile errors\ttestAdd1\ttestAdd2",
            "0\t0\t1\t1",
            "1\t0\t1\t0",
            "2\t1\t0\t0");
    static final List<String> EXPECTED_MULTIPLY_SIMPLE = List.of(
            "Name\tcompile errors\ttestMultiply1\ttestMultiply2",
            "0\t0\t1\t1",
            "1\t0\t1\t0",
            "2\t1\t0\t0");

    @Test
    public void testEclipseStructureEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        // TODO: make results available through API
        assertEquals(EXPECTED_ADD_SIMPLE, results);
    }

    @Test
    public void testEclipseStructureJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.JAVAC);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE, results);
    }

    @Test
    public void testMavenStructureEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, MVN_ROOT,
                ProjectStructure.MAVEN, Compiler.ECLIPSE);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE, results);
    }

    @Test
    public void testMavenStructureJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, MVN_ROOT,
                ProjectStructure.MAVEN, Compiler.JAVAC);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE, results);
    }

    @Test
    public void testPackageEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("multiply.MultiplyTest", "multiply.Multiply"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        assertEquals(EXPECTED_MULTIPLY_SIMPLE, results);
    }

    @Test
    public void testPackageJavac() throws IOException {
        var tasks = List.of(new Task("multiply.MultiplyTest", "multiply.Multiply"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.JAVAC);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        assertEquals(EXPECTED_MULTIPLY_SIMPLE, results);
    }

    @Test
    public void testUnrelatedCompileErrorEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "3");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t0\t1\t1",
                "3\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.JAVAC);
        grader.gradeOnly("0", "3");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t0\t1\t1",
                "3\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testCustomDir() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.setTestsDir(Path.of("tests-custom-dir"));
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompile errors\ttestAdd",
                "0\t0\t1",
                "1\t0\t0",
                "2\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testSingleDeduction() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "4");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tfix: changed signature\ttestAdd1\ttestAdd2",
                "0\t0\t1\t1",
                "4\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testMultipleDeductions() throws IOException {
        var tasks = List.of(new Task("DivideTest", "Divide"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "4");
        grader.run();
        var results = readAllLines(Path.of("results-DivideTest.tsv"));
        var expected = List.of(
                "Name\tfix: changed signature\tfix: wrong package\ttestDivide",
                "0\t0\t0\t1",
                "4\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testDeductionsPackage() throws IOException {
        var tasks = List.of(new Task("multiply.MultiplyTest", "Multiply"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "4");
        grader.run();
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tfix: changed signature\ttestMultiply1\ttestMultiply2",
                "0\t0\t1\t1",
                "4\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testTimeout() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, ECLIPSE_ROOT,
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "5"); // contains infinite loop
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tonly 1 repetitions\ttestAdd1\ttestAdd2\ttimeout",
                "0\t0\t1\t1\t0",
                "5\t1\t0\t0\t1");
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