package javagrader;

import static java.nio.file.Files.readAllLines;
import static javagrader.ProjectStructure.ECLIPSE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GraderTest {

    static final Path SUBM_ROOT = Path.of("test-submissions");

    @Test
    public void testSimpleTask() throws IOException {
        var tasks = List.of(new Task("SimpleTest", "Simple"));
        var grader = new Grader(tasks, SUBM_ROOT.resolve("simple"), ECLIPSE);
        grader.run();
        var results = readAllLines(Path.of("results-SimpleTest.tsv"));
        // TODO: make results available through API
        var expected = List.of(
                "Name\tcompile errors\ttestAdd1\ttestAdd2",
                "0\t0\t1\t1",
                "1\t0\t1\t0",
                "2\t1\t0\t0");
        assertEquals(expected, results);
    }
}
