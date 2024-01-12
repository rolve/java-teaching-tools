package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static org.junit.jupiter.api.Assertions.*;

public class InMemCompilationTest {

    @Test
    void compile() throws IOException {
        var sources = List.of(new InMemSource("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var result = InMemCompilation.compile(JAVAC, sources,
                ClassPath.empty(), System.out);
        assertFalse(result.errors());
        assertEquals(1, result.output().size());
        assertEquals("HelloWorld", result.output().get(0).getClassName());
    }

    @Test
    void diagnosticsOut() throws IOException {
        var sources = List.of(new InMemSource("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """));
        var diagnosticsOut = new ByteArrayOutputStream();
        var result = InMemCompilation.compile(JAVAC, sources,
                ClassPath.empty(), new PrintStream(diagnosticsOut));
        assertFalse(result.errors());
        assertEquals("", diagnosticsOut.toString());

        sources = List.of(new InMemSource("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!") // missing ;
                    }
                }
                """));
        result = InMemCompilation.compile(JAVAC, sources,
                ClassPath.empty(), new PrintStream(diagnosticsOut));
        assertTrue(result.errors());
        var diagnostics = diagnosticsOut.toString();
        assertTrue(diagnostics.contains("';' expected"), diagnostics);
    }
}
