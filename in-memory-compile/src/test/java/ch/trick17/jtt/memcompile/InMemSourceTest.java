package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemSourceTest {

    @Test
    void getPath() {
        var source = InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("HelloWorld.java", source.getPath());

        source = InMemSource.fromString("""
                public class Silly {}
                """);
        assertEquals("Silly.java", source.getPath());
    }

    @Test
    void getPathComment() {
        var source = InMemSource.fromString("""
                // public class Fake
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("HelloWorld.java", source.getPath());

        source = InMemSource.fromString("""
                /* public class Fake */
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("HelloWorld.java", source.getPath());
    }

    @Test
    void getPathPackage() {
        var source = InMemSource.fromString("""
                package greeting;
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("greeting/HelloWorld.java", source.getPath());

        source = InMemSource.fromString("""
                package silly;public class SillyTest {}
                """);
        assertEquals("silly/SillyTest.java", source.getPath());

        source = InMemSource.fromString("""
                package foo.bar.baz;
                public class Foo {}
                """);
        assertEquals("foo/bar/baz/Foo.java", source.getPath());
    }

    @Test
    void getPathMultipleClasses() {
        var source = InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                class Silly {}
                """);
        assertEquals("HelloWorld.java", source.getPath());

        source = InMemSource.fromString("""
                class Silly {}
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("HelloWorld.java", source.getPath());

        source = InMemSource.fromString("""
                class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                class Silly {}
                """);
        assertEquals("HelloWorld.java", source.getPath());

        source = InMemSource.fromString("""
                class Silly {}
                class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("Silly.java", source.getPath());
    }

    @Test
    void toUri() {
        var source = InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("mem:///HelloWorld.java", source.toUri().toString());
    }

    @Test
    void toUriPackage() {
        var source = InMemSource.fromString("""
                package greeting;
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("mem:///greeting/HelloWorld.java", source.toUri().toString());

        source = InMemSource.fromString("""
                package foo.bar.baz;
                public class Foo {}
                """);
        assertEquals("mem:///foo/bar/baz/Foo.java", source.toUri().toString());
    }

    @Test
    void supportsJava17() {
        assertDoesNotThrow(() -> InMemSource.fromString("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println(\"""
                                Hello, World!
                                \""");
                    }
                }
                """));
    }
}
