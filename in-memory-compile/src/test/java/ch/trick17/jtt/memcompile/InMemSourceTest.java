package ch.trick17.jtt.memcompile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemSourceTest {

    @Test
    void getFirstClassName() {
        var source = new InMemSource("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("HelloWorld", source.getFirstClassName());

        source = new InMemSource("""
                public class Silly {}
                """);
        assertEquals("Silly", source.getFirstClassName());
    }

    @Test
    void getFirstClassNamePackage() {
        var source = new InMemSource("""
                package greeting;
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("greeting.HelloWorld", source.getFirstClassName());

        source = new InMemSource("""
                package silly;public class SillyTest {}
                """);
        assertEquals("silly.SillyTest", source.getFirstClassName());
    }

    @Test
    void getFirstClassNameMultipleClasses() {
        var source = new InMemSource("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                public class Silly {}
                """);
        assertEquals("HelloWorld", source.getFirstClassName());
    }

    @Test
    void toUri() {
        var source = new InMemSource("""
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
        var source = new InMemSource("""
                package greeting;
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);
        assertEquals("mem:///greeting/HelloWorld.java", source.toUri().toString());
    }
}
