package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;

import javax.tools.DiagnosticCollector;
import java.nio.file.Path;
import java.util.List;

import static java.io.File.pathSeparator;
import static java.io.Writer.nullWriter;
import static java.lang.System.getProperty;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.junit.jupiter.api.Assertions.*;

public class InMemFileManagerTest {

    DiagnosticCollector<Object> diagnostics = new DiagnosticCollector<>();

    @Test
    void compileSingleClass() {
        var sources = List.of(new InMemSource("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var manager = new InMemFileManager(emptyList(), emptyList());
        var success = compile(manager, sources);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getOutput().size());
        assertEquals("HelloWorld", manager.getOutput().get(0).getClassName());
    }

    @Test
    void compileMultipleClasses() {
        var sources = List.of(new InMemSource("""
                package greeting;
                public class GreetingApp {
                    public static void main(String[] args) {
                        System.out.println(new Greeter().greet());
                    }
                }
                """), new InMemSource("""
                package greeting;
                public class Greeter {
                    public String greet() {
                        return "Hello, World!";
                    }
                }
                """));
        var manager = new InMemFileManager(emptyList(), emptyList());
        var success = compile(manager, sources);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(2, manager.getOutput().size());
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.GreetingApp")));
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.Greeter")));
    }

    @Test
    void compileMultipleClassesInSingleFile() {
        var sources = List.of(new InMemSource("""
                package greeting;
                public class GreetingApp {
                    public static void main(String[] args) {
                        System.out.println(new Greeter().greet());
                    }
                }
                class Greeter {
                    public String greet() {
                        return "Hello, World!";
                    }
                }
                """));
        var manager = new InMemFileManager(emptyList(), emptyList());
        var success = compile(manager, sources);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(2, manager.getOutput().size());
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.GreetingApp")));
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.Greeter")));
    }

    @Test
    void compileWithClassPath() {
        var sources = List.of(new InMemSource("""
                import static java.util.Collections.emptyList;
                import ch.trick17.jtt.memcompile.InMemFileManager;
                
                public class InMemoryFileManagerClient {
                    public static void main(String[] args) {
                        var manager = new InMemFileManager(emptyList(), emptyList());
                        System.out.println(manager.getOutput().size());
                    }
                }
                """));

        var classPath = stream(getProperty("java.class.path").split(pathSeparator))
                .map(Path::of)
                .toList();
        var manager = new InMemFileManager(emptyList(), classPath);
        var success = compile(manager, sources);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getOutput().size());

        // with empty classpath, compilation should fail
        manager = new InMemFileManager(emptyList(), emptyList());
        success = compile(manager, sources);
        assertFalse(success, diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertNotEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(0, manager.getOutput().size());
    }

    private boolean compile(InMemFileManager manager, List<InMemSource> sources) {
        var compiler = getSystemJavaCompiler();
        var task = compiler.getTask(nullWriter(), manager, diagnostics,
                null, null, sources);
        return task.call();
    }
}
