package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static ch.trick17.jtt.memcompile.ClassPath.fromMemory;
import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class InMemCompilationTest {

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compile(Compiler compiler) throws IOException {
        var sources = List.of(InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var result = InMemCompilation.compile(compiler, sources, ClassPath.empty());
        assertEquals(emptyList(), result.errors());
        assertEquals(1, result.output().size());
        assertEquals("HelloWorld", result.output().getFirst().getClassName());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compilePackage(Compiler compiler) throws IOException {
        var sources = List.of(InMemSource.fromString("""
                package foo.bar;
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var result = InMemCompilation.compile(compiler, sources, ClassPath.empty());
        assertEquals(emptyList(), result.errors());
        assertEquals(1, result.output().size());
        assertEquals("foo.bar.HelloWorld", result.output().getFirst().getClassName());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileImportDifferentPackage(Compiler compiler) throws IOException {
        var sources = List.of(InMemSource.fromString("""
                package foo.bar;
                
                import foo.bar.baz.Greeter;
                
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println(Greeter.greeting());
                    }
                }
                """), InMemSource.fromString("""
                package foo.bar.baz;
                
                public class Greeter {
                    public static String greeting() {
                        return "Hello, World!";
                    }
                }
                """));
        var result = InMemCompilation.compile(compiler, sources, ClassPath.empty());
        assertEquals(emptyList(), result.errors());
        assertEquals(2, result.output().size());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileImportDifferentPackageClassPath(Compiler compiler) throws IOException {
        var sources = List.of(InMemSource.fromString("""
                package foo.bar.baz;
                
                public class Greeter {
                    public static String greeting() {
                        return "Hello, World!";
                    }
                }
                """));
        var result = InMemCompilation.compile(compiler, sources, ClassPath.empty());
        assertEquals(emptyList(), result.errors());
        assertEquals(1, result.output().size());

        sources = List.of(InMemSource.fromString("""
                package foo.bar;
                
                import foo.bar.baz.Greeter;
                
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println(Greeter.greeting());
                    }
                }
                """));
        result = InMemCompilation.compile(compiler, sources, fromMemory(result.output()));
        assertEquals(emptyList(), result.errors());
        assertEquals(1, result.output().size());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void compileErrors(Compiler compiler) throws IOException {
        var sources = List.of(InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var diagnosticsOut = new ByteArrayOutputStream();
        var result = InMemCompilation.compile(compiler, sources, ClassPath.empty());
        assertEquals(emptyList(), result.errors());

        sources = List.of(new InMemSource("HelloWorld.java", """
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!") // missing ;
                    }
                }
                """));
        result = InMemCompilation.compile(compiler, sources, ClassPath.empty());
        assertEquals(1, result.errors().size());
        var error = result.errors().getFirst();
        assertTrue(error.contains(";"), error);
    }

    @Test
    void partialCompilation() throws Exception {
        var sources = List.of(InMemSource.fromString("""
                public class Partial {
                    public static int foo() {
                        return 42;
                    }
                    public static int bar() {
                        // compile error: missing return
                    }
                }
                """));
        var result = InMemCompilation.compile(ECLIPSE, sources, ClassPath.empty());

        // class was still compiled and can be loaded and partially used
        assertEquals(1, result.output().size());
        try (var loader = new InMemClassLoader(fromMemory(result.output()),
                getClass().getClassLoader())) {
            var partial = loader.loadClass("Partial");
            var foo = partial.getMethod("foo").invoke(null);
            assertEquals(42, foo);

            // but invoking bar throws an Error
            var e = assertThrows(InvocationTargetException.class, () -> {
                partial.getMethod("bar").invoke(null);
            });
            assertInstanceOf(Error.class, e.getCause());
            assertTrue(e.getCause().getMessage().contains("Unresolved compilation problem"));
        }
    }

    @Test
    void partialCompilationAnonymousClass() throws Exception {
        // Compile errors in lambda expressions prevent partial compilation.
        // Test that the workaround with anonymous classes works.

        var sources = List.of(InMemSource.fromString("""
                import java.util.List;
                import java.util.function.Consumer;
                
                public class Partial {
                    public static int foo() {
                        return 42;
                    }
                    public static void bar() {
                        List.of(1, 2, 3).forEach(new Consumer<>() {
                            public void accept(Integer i) {
                                baz(); // compile error: baz() is not defined
                            }
                        });
                    }
                }
                """));
        var result = InMemCompilation.compile(ECLIPSE, sources, ClassPath.empty());

        // class was still compiled and can be loaded and partially used
        assertNotEquals(emptyList(), result.output());
        try (var loader = new InMemClassLoader(fromMemory(result.output()),
                getClass().getClassLoader())) {
            var partial = loader.loadClass("Partial");
            var foo = partial.getMethod("foo").invoke(null);
            assertEquals(42, foo);

            // but invoking bar throws an Error
            var e = assertThrows(InvocationTargetException.class, () -> {
                partial.getMethod("bar").invoke(null);
            });
            assertInstanceOf(Error.class, e.getCause());
            assertTrue(e.getCause().getMessage().contains("Unresolved compilation problem"));
        }
    }
}
