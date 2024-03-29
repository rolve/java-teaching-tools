package io;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(3)
public class ExtractNumbersTest {

    /**
     * <code>extractNumbers</code> mit einem leeren Text aufrufen und prüfen, dass eine
     * leere Liste zurückgegeben wird.
     */
    @Order(1)
    @Test
    public void testEmptyText() throws IOException {
        var text = "";
        var numbers = IOTasks.extractNumbers(asStream(text));
        assertEquals(emptyList(), numbers);
    }

    /**
     * <code>extractNumbers</code> mit einem Text aufrufen, der keine Zahlen enthält, und
     * prüfen, dass eine leere Liste zurückgegeben wird.
     */
    @Order(2)
    @Test
    public void testNoNumbers() throws IOException {
        var text = """
                This text contains no numbers,
                not a single one.
                                
                Still no numbers down here...
                """;
        var numbers = IOTasks.extractNumbers(asStream(text));
        assertEquals(emptyList(), numbers);
    }

    /**
     * <code>extractNumbers</code> mit einem Text aufrufen, der ganze Zahlen enthält, und
     * prüfen, dass die Zahlen in der richtigen Reihenfolge zurückgegeben
     * werden.
     */
    @Order(3)
    @Test
    public void testInts() throws IOException {
        var text = """
                This text contains the number 6 and
                also 9 so that makes 2 numbers, wait
                I mean 3 oh forget it...
                """;
        var numbers = IOTasks.extractNumbers(asStream(text));
        assertEquals(List.of(6.0, 9.0, 2.0, 3.0), numbers);
    }

    /**
     * <code>extractNumbers</code> mit einem Text aufrufen, der reelle Zahlen enthält, und
     * prüfen, dass die Zahlen in der richtigen Reihenfolge zurückgegeben
     * werden.
     */
    @Order(4)
    @Test
    public void testDoubles() throws IOException {
        var text = """
                This text contains the number 2.5 and
                also 10.1 so that makes 2.0 numbers, wait
                I mean 3.0 oh forget it...
                """;
        var numbers = IOTasks.extractNumbers(asStream(text));
        assertEquals(List.of(2.5, 10.1, 2.0, 3.0), numbers);
    }

    /**
     * <code>extractNumbers</code> mit einem Text aufrufen, der negative Zahlen enthält,
     * und prüfen, dass die Zahlen in der richtigen Reihenfolge zurückgegeben
     * werden.
     */
    @Order(5)
    @Test
    public void testNegative() throws IOException {
        var text = """
                This text contains the number -42.7 and
                also -0.1234 so that makes 2 numbers, wait
                I mean 3 oh forget it...
                """;
        var numbers = IOTasks.extractNumbers(asStream(text));
        assertEquals(List.of(-42.7, -0.1234, 2.0, 3.0), numbers);
    }

    /**
     * Prüfen, dass <code>extractNumbers</code> den übergebenen InputStream schliesst.
     */
    @Order(6)
    @Test
    public void testClose() throws IOException {
        var text = """
                This text contains the number 2.5 and
                also 10.1 so that makes 2.0 numbers, wait
                I mean 3.0 oh forget it...
                """;
        var closed = new int[]{0};
        var in = new ByteArrayInputStream(text.getBytes(UTF_8)) {
            public void close() {
                closed[0]++;
            }
        };
        IOTasks.extractNumbers(in);
        assertEquals(1, closed[0]);
    }

    private InputStream asStream(String text) {
        return new ByteArrayInputStream(text.getBytes(UTF_8));
    }
}
