package javagrader;

import static java.nio.file.Files.readAllLines;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GraderTest {

    static final Path SUBM_ROOT = Path.of("test-submissions");

    static final List<String> EXPECTED_ADD_SIMPLE = List.of(
            "Name\tcompile errors\ttestAdd1\ttestAdd2",
            "0\t0\t1\t1",
            "1\t0\t1\t0",
            "2\t1\t0\t0");

    @Test
    public void testSimpleEclipseCompiler() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, SUBM_ROOT.resolve("add"),
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        // TODO: make results available through API
        assertEquals(EXPECTED_ADD_SIMPLE, results);
    }

    @Test
    public void testSimpleJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, SUBM_ROOT.resolve("add"),
                ProjectStructure.ECLIPSE, Compiler.JAVAC);
        grader.gradeOnly("0", "1", "2");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE, results);
    }

    @Test
    public void testUnrelatedCompileErrorEclipse() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, SUBM_ROOT.resolve("add"),
                ProjectStructure.ECLIPSE, Compiler.ECLIPSE);
        grader.gradeOnly("3");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompile errors\ttestAdd1\ttestAdd2",
                "3\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorJavac() throws IOException {
        var tasks = List.of(new Task("AddTest", "Add"));
        var grader = new Grader(tasks, SUBM_ROOT.resolve("add"),
                ProjectStructure.ECLIPSE, Compiler.JAVAC);
        grader.gradeOnly("3");
        grader.run();
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompile errors",
                "3\t1");
        assertEquals(expected, results);
    }
}
