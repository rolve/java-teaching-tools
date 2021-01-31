package ch.trick17.jtt.grader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskTest {

    @Test
    public void testFromClassName() throws IOException {
        var task = Task.fromClassName("AddTest");
        assertEquals("AddTest", task.testClassName());
        var path = Path.of("AddTest.java");
        var code = readAllBytes(Path.of("tests").resolve(path));

        var filesToCopy = task.filesToCopy();
        assertEquals(1, filesToCopy.size());
        var entry = filesToCopy.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertArrayEquals(code, entry.getValue());
    }

    @Test
    public void testFromClassNamePackage() throws IOException {
        var task = Task.fromClassName("multiply.MultiplyTest");
        assertEquals("multiply.MultiplyTest", task.testClassName());
        var path = Path.of("multiply/MultiplyTest.java");
        var code = readAllBytes(Path.of("tests").resolve(path));

        var filesToCopy = task.filesToCopy();
        assertEquals(1, filesToCopy.size());
        var entry = filesToCopy.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertArrayEquals(code, entry.getValue());
    }

    @Test
    public void testFromClassNameCustomDir() throws IOException {
        var task = Task.fromClassName("AddTest", Path.of("tests-custom-dir"));
        assertEquals("AddTest", task.testClassName());
        var path = Path.of("AddTest.java");
        var code = readAllBytes(Path.of("tests-custom-dir").resolve(path));

        var filesToCopy = task.filesToCopy();
        assertEquals(1, filesToCopy.size());
        var entry = filesToCopy.entrySet().iterator().next();
        assertEquals(path, entry.getKey());
        assertArrayEquals(code, entry.getValue());
    }

    @Test
    public void testFromString() {
        var task = Task.fromString("public class SillyTest {}");
        var filesToCopy = task.filesToCopy();
        assertEquals(1, filesToCopy.size());
        var entry = filesToCopy.entrySet().iterator().next();
        assertEquals(Path.of("SillyTest.java"), entry.getKey());
        assertArrayEquals("public class SillyTest {}".getBytes(UTF_8), entry.getValue());
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
            var filesToCopy = task.filesToCopy();
            assertEquals(1, filesToCopy.size());
            var entry = filesToCopy.entrySet().iterator().next();
            assertEquals(Path.of("silly/SillyTest.java"), entry.getKey());
            assertArrayEquals(code.getBytes(UTF_8), entry.getValue());
        }
    }
}
