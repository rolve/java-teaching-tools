package ch.trick17.jtt.memcompile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
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
        var result = InMemCompilation.compile(compiler, sources,
                ClassPath.empty(), System.out);
        assertFalse(result.errors());
        assertEquals(1, result.output().size());
        assertEquals("HelloWorld", result.output().get(0).getClassName());
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
        var result = InMemCompilation.compile(compiler, sources,
                ClassPath.empty(), System.out);
        assertFalse(result.errors());
        assertEquals(1, result.output().size());
        assertEquals("foo.bar.HelloWorld", result.output().get(0).getClassName());
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
        var result = InMemCompilation.compile(compiler, sources,
                ClassPath.empty(), System.out);
        assertFalse(result.errors());
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
        var result = InMemCompilation.compile(compiler, sources,
                ClassPath.empty(), System.out);
        assertFalse(result.errors());
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
        result = InMemCompilation.compile(compiler, sources,
                ClassPath.fromMemory(result.output()), System.out);
        assertFalse(result.errors());
        assertEquals(1, result.output().size());
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void diagnosticsOut(Compiler compiler) throws IOException {
        var sources = List.of(InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var diagnosticsOut = new ByteArrayOutputStream();
        var result = InMemCompilation.compile(compiler, sources,
                ClassPath.empty(), new PrintStream(diagnosticsOut));
        assertFalse(result.errors());
        assertEquals("", diagnosticsOut.toString());

        sources = List.of(new InMemSource("HelloWorld.java", """
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!") // missing ;
                    }
                }
                """));
        result = InMemCompilation.compile(compiler, sources,
                ClassPath.empty(), new PrintStream(diagnosticsOut));
        assertTrue(result.errors());
        var diagnostics = diagnosticsOut.toString();
        assertTrue(diagnostics.contains(";"), diagnostics);
    }
}
