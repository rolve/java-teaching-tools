package ch.trick17.jtt.grader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.readString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskTest {

    static final Path TEST_SRC_DIR = Path.of("tests");

    @Test
    public void testFromClassName() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR);
        assertEquals("AddTest", task.testClassName());
        var path = Path.of("AddTest.java");
        var code = readString(Path.of("tests").resolve(path));

        var testClasses = task.testClasses();
        assertEquals(1, testClasses.size());
        var entry = testClasses.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertEquals(code, entry.getValue());
    }

    @Test
    public void testFromClassNamePackage() throws IOException {
        var task = Task.fromClassName("multiply.MultiplyTest", TEST_SRC_DIR);
        assertEquals("multiply.MultiplyTest", task.testClassName());
        var path = Path.of("multiply/MultiplyTest.java");
        var code = readString(Path.of("tests").resolve(path));

        var testClasses = task.testClasses();
        assertEquals(1, testClasses.size());
        var entry = testClasses.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertEquals(code, entry.getValue());
    }

    @Test
    public void testFromClassNameDefaultDir() throws IOException {
        var task = Task.fromClassName("ch.trick17.jtt.grader.TaskTest");
        assertEquals("ch.trick17.jtt.grader.TaskTest", task.testClassName());
        var path = Path.of("ch/trick17/jtt/grader/TaskTest.java");
        var code = readString(Path.of("src/test/java").resolve(path));

        var testClasses = task.testClasses();
        assertEquals(1, testClasses.size());
        var entry = testClasses.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertEquals(code, entry.getValue());
    }

    @Test
    public void testFrom() throws IOException {
        var task = Task.from(TaskTest.class);
        assertEquals("ch.trick17.jtt.grader.TaskTest", task.testClassName());
        var path = Path.of("ch/trick17/jtt/grader/TaskTest.java");
        var code = readString(Path.of("src/test/java").resolve(path));

        var testClasses = task.testClasses();
        assertEquals(1, testClasses.size());
        var entry = testClasses.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertEquals(code, entry.getValue());
    }

    @Test
    public void testFromString() {
        var task = Task.fromString("public class SillyTest {}");
        var testClasses = task.testClasses();
        assertEquals(1, testClasses.size());
        var entry = testClasses.entrySet().iterator().next();
        assertEquals(Path.of("SillyTest.java"), entry.getKey());
        assertEquals("public class SillyTest {}", entry.getValue());
    }

    @Test
    public void testFromStringPackage() {
        String[] codes = {
                "package silly;\n" +
                "public class SillyTest {\n" +
                "}",
                "package silly;public class SillyTest {}",
                "package silly;\n" +
                "class SillyTest {}\n" +
                "class Other {}"
        };

        for (var code : codes) {
            var task = Task.fromString(code);
            var testClasses = task.testClasses();
            assertEquals(1, testClasses.size());
            var entry = testClasses.entrySet().iterator().next();
            assertEquals(Path.of("silly/SillyTest.java"), entry.getKey());
            assertEquals(code, entry.getValue());
        }
    }
}
