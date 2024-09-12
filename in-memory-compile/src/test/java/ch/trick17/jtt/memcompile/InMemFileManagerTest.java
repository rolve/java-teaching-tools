package ch.trick17.jtt.memcompile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.tools.DiagnosticCollector;
import java.util.List;

import static java.io.Writer.nullWriter;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;

public class InMemFileManagerTest {

    DiagnosticCollector<Object> diagnostics = new DiagnosticCollector<>();

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileSingleClass(Compiler compiler) {
        var sources = List.of(InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var manager = new InMemFileManager(sources, ClassPath.empty());
        var success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getOutput().size());
        assertEquals("HelloWorld", manager.getOutput().getFirst().getClassName());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileMultipleClasses(Compiler compiler) {
        var sources = List.of(InMemSource.fromString("""
                package greeting;
                public class GreetingApp {
                    public static void main(String[] args) {
                        System.out.println(new Greeter().greet());
                    }
                }
                """), InMemSource.fromString("""
                package greeting;
                public class Greeter {
                    public String greet() {
                        return "Hello, World!";
                    }
                }
                """));
        var manager = new InMemFileManager(sources, ClassPath.empty());
        var success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(2, manager.getOutput().size());
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.GreetingApp")));
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.Greeter")));
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileMultipleClassesInSingleFile(Compiler compiler) {
        var sources = List.of(InMemSource.fromString("""
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
        var manager = new InMemFileManager(sources, ClassPath.empty());
        var success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(2, manager.getOutput().size());
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.GreetingApp")));
        assertTrue(manager.getOutput().stream()
                .anyMatch(f -> f.getClassName().equals("greeting.Greeter")));
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileWithFileClassPath(Compiler compiler) {
        var sources = List.of(InMemSource.fromString("""
                import java.io.IOException;
                import ch.trick17.jtt.memcompile.ClassPath;
                import ch.trick17.jtt.memcompile.InMemFileManager;
                
                import static java.util.Collections.emptyList;
                
                public class InMemoryFileManagerClient {
                    public static void main(String[] args) throws IOException {
                        try (var manager = new InMemFileManager(emptyList(), ClassPath.empty())) {
                            System.out.println(manager.getOutput().size());
                        }
                    }
                }
                """));

        var manager = new InMemFileManager(sources, ClassPath.fromCurrent());
        var success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getOutput().size());

        // with empty classpath, compilation should fail
        manager = new InMemFileManager(sources, ClassPath.empty());
        success = compile(manager, sources, compiler);
        assertFalse(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertNotEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(0, manager.getOutput().size());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileWithMemClassPath(Compiler compiler) {
        var sources = List.of(InMemSource.fromString("""
                package greeting;
                public class Greeter {
                    public String greet() {
                        return "Hello, World!";
                    }
                }
                """));

        var manager = new InMemFileManager(sources, ClassPath.empty());
        var success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getOutput().size());

        sources = List.of(InMemSource.fromString("""
                package greeting;
                public class GreetingApp {
                    public static void main(String[] args) {
                        System.out.println(new Greeter().greet());
                    }
                }
                """));
        manager = new InMemFileManager(sources, ClassPath.fromMemory(manager.getOutput()));
        success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(1, manager.getOutput().size());
    }

    private boolean compile(InMemFileManager manager, List<InMemSource> sources,
                            Compiler compiler) {
        var task = compiler.create().getTask(nullWriter(), manager, diagnostics,
                null, null, sources);
        return task.call();
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileJava21Features(Compiler compiler) {
        var sources = List.of(InMemSource.fromString("""
                public class HelloWorld {
                    public void foo(Stuff stuff) {
                        System.out.println(switch (stuff) {
                            case Thing(var name) -> name;
                            case OtherThing(var id) -> id;
                        });
                    }
                    public sealed interface Stuff {}
                    public record Thing(String name) implements Stuff {}
                    public record OtherThing(int id) implements Stuff {}
                }
                """));
        var manager = new InMemFileManager(sources, ClassPath.empty());
        var success = compile(manager, sources, compiler);
        assertTrue(success, diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(ROOT))
                .collect(joining("\n")));
        assertEquals(0, diagnostics.getDiagnostics().size());
        assertEquals(4, manager.getOutput().size());
    }
}
