package ch.trick17.jtt.grader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static ch.trick17.jtt.grader.GraderTest.TEST_SRC_DIR;
import static java.nio.file.Files.readString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskTest {

    @Test
    public void testFromClassName() throws IOException {
        var task = Grader.Task.fromClassName("AddTest", TEST_SRC_DIR);
        assertEquals("AddTest", task.testClassName());
        var path = Path.of("AddTest.java");
        var code = readString(Path.of("tests").resolve(path));

        var testClasses = task.testSources();
        assertEquals(1, testClasses.size());
        assertEquals(code, testClasses.get(0).getContent());
    }

    @Test
    public void testFromClassNamePackage() throws IOException {
        var task = Grader.Task.fromClassName("multiply.MultiplyTest", TEST_SRC_DIR);
        assertEquals("multiply.MultiplyTest", task.testClassName());
        var path = Path.of("multiply/MultiplyTest.java");
        var code = readString(Path.of("tests").resolve(path));

        var testClasses = task.testSources();
        assertEquals(1, testClasses.size());
        assertEquals(code, testClasses.get(0).getContent());
    }

    @Test
    public void testFromClassNameDefaultDir() throws IOException {
        var task = Grader.Task.fromClassName("ch.trick17.jtt.grader.TaskTest");
        assertEquals("ch.trick17.jtt.grader.TaskTest", task.testClassName());
        var path = Path.of("ch/trick17/jtt/grader/TaskTest.java");
        var code = readString(Path.of("src/test/java").resolve(path));

        var testClasses = task.testSources();
        assertEquals(1, testClasses.size());
        assertEquals(code, testClasses.get(0).getContent());
    }

    @Test
    public void testFrom() throws IOException {
        var task = Grader.Task.from(TaskTest.class);
        assertEquals("ch.trick17.jtt.grader.TaskTest", task.testClassName());
        var path = Path.of("ch/trick17/jtt/grader/TaskTest.java");
        var code = readString(Path.of("src/test/java").resolve(path));

        var testClasses = task.testSources();
        assertEquals(1, testClasses.size());
        assertEquals(code, testClasses.get(0).getContent());
    }

    @Test
    public void testFromString() {
        var task = Grader.Task.fromString("public class SillyTest {}");
        var testClasses = task.testSources();
        assertEquals(1, testClasses.size());
        assertEquals("public class SillyTest {}", testClasses.get(0).getContent());
    }
}
