package ch.trick17.jtt.grader;

import org.junit.jupiter.api.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import java.util.Map;

import static java.io.Writer.nullWriter;
import static java.util.stream.Collectors.joining;
import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryFileManagerTest {

    JavaCompiler compiler = getSystemJavaCompiler();
    DiagnosticCollector<Object> diagnostics = new DiagnosticCollector<>();

    @Test
    void compileSingleClass() {
        var manager = new InMemoryFileManager(Map.of(
                "HelloWorld",
                """
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var task = compiler.getTask(nullWriter(), manager, diagnostics,
                null, null, manager.getSourceFiles());
        assertTrue(task.call(), diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getClassFiles().size());
        assertTrue(manager.getClassFiles().containsKey("HelloWorld"));
    }

    @Test
    void compileMultipleClasses() {
        var manager = new InMemoryFileManager(Map.of(
                "greeting.GreetingApp",
                """
                package greeting;
                public class GreetingApp {
                    public static void main(String[] args) {
                        System.out.println(new Greeter().greet());
                    }
                }
                """,
                "greeting.Greeter",
                """
                package greeting;
                public class Greeter {
                    public String greet() {
                        return "Hello, World!";
                    }
                }
                """));
        var task = compiler.getTask(nullWriter(), manager, diagnostics,
                null, null, manager.getSourceFiles());
        assertTrue(task.call(), diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(2, manager.getClassFiles().size());
        assertTrue(manager.getClassFiles().containsKey("greeting.GreetingApp"));
        assertTrue(manager.getClassFiles().containsKey("greeting.Greeter"));
    }

    @Test
    void compileMultipleClassesInSingleFile() {
        var manager = new InMemoryFileManager(Map.of(
                "greeting.GreetingApp",
                """
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
        var task = compiler.getTask(nullWriter(), manager, diagnostics,
                null, null, manager.getSourceFiles());
        assertTrue(task.call(), diagnostics.getDiagnostics().stream()
                .map(Object::toString)
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(2, manager.getClassFiles().size());
        assertTrue(manager.getClassFiles().containsKey("greeting.GreetingApp"));
        assertTrue(manager.getClassFiles().containsKey("greeting.Greeter"));
    }
}
