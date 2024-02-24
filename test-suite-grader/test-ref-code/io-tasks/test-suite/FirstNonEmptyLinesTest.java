package io;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@Order(1)
public class FirstNonEmptyLinesTest {

    /**
     * <code>firstNonEmptyLines</code> mit <code>n = 0</code> aufrufen und prüfen, dass eine leere
     * Liste zurückgegeben wird.
     */
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

    /**
     * <code>firstNonEmptyLines</code> mit <code>n = 1</code> aufrufen und prüfen, dass die erste
     * (nicht-leere) Zeile zurückgegeben wird.
     */
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

    /**
     * <code>firstNonEmptyLines</code> mit verschiedenen <code>n</code> aufrufen und prüfen, dass
     * entsprechend viele (nicht-leere) Zeilen zurückgegeben werden.
     */
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

    /**
     * <code>firstNonEmptyLines</code> mit einem Text aufrufen, der leere Zeilen enthält,
     * und prüfen, dass die leeren Zeilen ignoriert werden.
     */
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

    /**
     * <code>firstNonEmptyLines</code> mit einem Text aufrufen, der weniger als <code>n</code> Zeilen
     * enthält, und prüfen, dass alle Zeilen zurückgegeben werden.
     */
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

    /**
     * <code>firstNonEmptyLines</code> mit einem Text aufrufen, der Nicht-ASCII-Zeichen
     * enthält, und prüfen, dass die Zeichen korrekt decodiert werden.
     */
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

    /**
     * Prüfen, dass <code>firstNonEmptyLines</code> den übergebenen InputStream schliesst.
     */
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

    /**
     * Prüfen, dass <code>firstNonEmptyLines</code> den übergebenen InputStream schliesst,
     * auch wenn eine Exception auftritt.
     */
    @Order(8)
    @Test
    public void testCloseException() {
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
