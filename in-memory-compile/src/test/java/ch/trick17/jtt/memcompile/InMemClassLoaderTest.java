package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemClassLoaderTest {

    @Test
    void loadClass() throws Exception {
        InMemClassFile classFile;
        try (var in = Greeter.class.getResourceAsStream("Greeter.class")) {
            var bytes = in.readAllBytes();
            classFile = new InMemClassFile(Greeter.class.getName(), bytes);
        }

        var loader = new InMemClassLoader(List.of(classFile), emptyList(),
                getSystemClassLoader());
        var greeter = loader.loadClass(Greeter.class.getName(), false);

        var greet = greeter.getDeclaredMethod("greet");
        greet.setAccessible(true); // different class loader, hence no access
        assertEquals("Hello, World!", greet.invoke(null));
    }
}

class Greeter {
    static String greet() {
        return "Hello, World!";
    }
}
