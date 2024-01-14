package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ch.trick17.jtt.memcompile.ClassPath.empty;
import static java.lang.ClassLoader.getPlatformClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SandboxClassLoaderTest {

    @Test
    void fileClassPathSandboxed() throws Exception {
        var classPath = ClassPath.fromCurrent();
        var loader = new SandboxClassLoader(classPath, empty(),
                Whitelist.getDefault(), false, getPlatformClassLoader());
        var greeter = loader.loadClass(Greeter.class.getName());
        var greet = greeter.getDeclaredMethod("greet");
        greet.setAccessible(true);
        assertEquals("Hello, World!", greet.invoke(null));
    }

    @Test
    void fileClassPathSupport() throws Exception {
        var classPath = ClassPath.fromCurrent();
        var loader = new SandboxClassLoader(empty(), classPath,
                Whitelist.getDefault(), false, getPlatformClassLoader());
        var greeter = loader.loadClass(Greeter.class.getName());
        var greet = greeter.getDeclaredMethod("greet");
        greet.setAccessible(true);
        assertEquals("Hello, World!", greet.invoke(null));
    }

    @Test
    void memClassPathSandboxed() throws Exception {
        InMemClassFile classFile;
        try (var in = Greeter.class.getResourceAsStream("Greeter.class")) {
            var bytes = in.readAllBytes();
            classFile = new InMemClassFile(Greeter.class.getName(), bytes);
        }

        var classPath = ClassPath.fromMemory(List.of(classFile));
        var loader = new SandboxClassLoader(classPath, empty(),
                Whitelist.getDefault(), false, getPlatformClassLoader());
        var greeter = loader.loadClass(Greeter.class.getName());
        var greet = greeter.getDeclaredMethod("greet");
        greet.setAccessible(true);
        assertEquals("Hello, World!", greet.invoke(null));

        assertThrows(ClassNotFoundException.class, () -> {
            loader.loadClass("ch.trick17.jtt.sandbox.SandboxClassLoaderTest");
        });
    }

    @Test
    void memClassPathSupport() throws Exception {
        InMemClassFile classFile;
        try (var in = Greeter.class.getResourceAsStream("Greeter.class")) {
            var bytes = in.readAllBytes();
            classFile = new InMemClassFile(Greeter.class.getName(), bytes);
        }

        var classPath = ClassPath.fromMemory(List.of(classFile));
        var loader = new SandboxClassLoader(empty(), classPath,
                Whitelist.getDefault(), false, getPlatformClassLoader());
        var greeter = loader.loadClass(Greeter.class.getName());
        var greet = greeter.getDeclaredMethod("greet");
        greet.setAccessible(true);
        assertEquals("Hello, World!", greet.invoke(null));

        assertThrows(ClassNotFoundException.class, () -> {
            loader.loadClass("ch.trick17.jtt.sandbox.SandboxClassLoaderTest");
        });
    }
}

class Greeter {
    static String greet() {
        return "Hello, World!";
    }
}
