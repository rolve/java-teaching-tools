package io;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class FirstNonEmptyLinesTest {

    @Order(1)
    @Test
    public void testZero() throws IOException {
        var text = """
                First
                Second
                Third
                Fourth
                """;
        var list = IOTasks.firstNonEmptyLines(asStream(text), 0);
        assertEquals(emptyList(), list);
    }

    @Order(2)
    @Test
    public void testOne() throws IOException {
        var text = """
                First
                Second
                Third
                Fourth
                """;
        var first = IOTasks.firstNonEmptyLines(asStream(text), 1);
        assertEquals(List.of("First"), first);
    }

    @Order(3)
    @Test
    public void testBasic() throws IOException {
        var text = """
                First
                Second
                Third
                Fourth
                """;
        var first2 = IOTasks.firstNonEmptyLines(asStream(text), 2);
        assertEquals(List.of("First", "Second"), first2);

        var first3 = IOTasks.firstNonEmptyLines(asStream(text), 3);
        assertEquals(List.of("First", "Second", "Third"), first3);

        var first4 = IOTasks.firstNonEmptyLines(asStream(text), 4);
        assertEquals(List.of("First", "Second", "Third", "Fourth"), first4);
    }

    @Order(4)
    @Test
    public void testEmptyLines() throws IOException {
        var text = """
                
                First
                Second
                
                Third
                
                
                Fourth
                """;
        var first2 = IOTasks.firstNonEmptyLines(asStream(text), 2);
        assertEquals(List.of("First", "Second"), first2);

        var first3 = IOTasks.firstNonEmptyLines(asStream(text), 3);
        assertEquals(List.of("First", "Second", "Third"), first3);

        var first4 = IOTasks.firstNonEmptyLines(asStream(text), 4);
        assertEquals(List.of("First", "Second", "Third", "Fourth"), first4);
    }

    @Order(5)
    @Test
    public void testTooFew() throws IOException {
        var text = """
                    First
                    
                    Second
                    """;
        var first2 = IOTasks.firstNonEmptyLines(asStream(text), 3);
        assertEquals(List.of("First", "Second"), first2);

        var first3 = IOTasks.firstNonEmptyLines(asStream(text), 4);
        assertEquals(List.of("First", "Second"), first3);

        var first4 = IOTasks.firstNonEmptyLines(asStream(text), 100);
        assertEquals(List.of("First", "Second"), first4);
    }

    @Order(6)
    @Test
    public void testEncoding() throws IOException {
        var text = """
                Hö?
                Straßenfußball
                «Hallo»
                ¡Dale caña!
                """;
        var first2 = IOTasks.firstNonEmptyLines(asStream(text), 2);
        assertEquals(List.of("Hö?", "Straßenfußball"), first2);

        var first3 = IOTasks.firstNonEmptyLines(asStream(text), 3);
        assertEquals(List.of("Hö?", "Straßenfußball", "«Hallo»"), first3);

        var first4 = IOTasks.firstNonEmptyLines(asStream(text), 4);
        assertEquals(List.of("Hö?", "Straßenfußball", "«Hallo»", "¡Dale caña!"), first4);
    }

    @Order(7)
    @Test
    public void testClose() throws IOException {
        var text = """
                First
                Second
                """;
        var closed = new int[]{0};
        var in = new ByteArrayInputStream(text.getBytes(ISO_8859_1)) {
            public void close() {
                closed[0]++;
            }
        };
        IOTasks.firstNonEmptyLines(in, 2);
        assertEquals(1, closed[0]);
    }

    @Order(8)
    @Test
    public void testCloseException() throws IOException {
        var text = """
                First
                Second
                """;
        var closed = new int[]{0};
        var in = new InputStream() {
            public int read() throws IOException {
                throw new IOException();
            }
            public void close() {
                closed[0]++;
            }
        };
        assertThrows(IOException.class, () -> {
            IOTasks.firstNonEmptyLines(in, 2);
        });
        assertEquals(1, closed[0]);
    }

    private InputStream asStream(String text) {
        return new ByteArrayInputStream(text.getBytes(ISO_8859_1));
    }
}
