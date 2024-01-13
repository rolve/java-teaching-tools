package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.ClassLoader.getPlatformClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InMemClassLoaderTest {

    @Test
    void loadClass() throws Exception {
        InMemClassFile classFile;
        try (var in = Greeter.class.getResourceAsStream("Greeter.class")) {
            var bytes = in.readAllBytes();
            classFile = new InMemClassFile(Greeter.class.getName(), bytes);
        }

        var loader = new InMemClassLoader(ClassPath.fromMemory(List.of(classFile)),
                getPlatformClassLoader());
        var greeter = loader.loadClass(Greeter.class.getName());

        var greet = greeter.getDeclaredMethod("greet");
        greet.setAccessible(true); // different class loader, hence no access
        assertEquals("Hello, World!", greet.invoke(null));
    }

    @Test
    void loadClassNotFound() {
        var loader = new InMemClassLoader(ClassPath.empty(), getPlatformClassLoader());
        assertThrows(ClassNotFoundException.class, () -> {
            loader.loadClass(Greeter.class.getName());
        });
    }
}

class Greeter {
    static String greet() {
        return "Hello, World!";
    }
}
