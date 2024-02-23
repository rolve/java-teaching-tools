package io;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(2)
public class WritePowersOfTwoTest {

    /**
     * `writePowersOfTwo` mit `n = 0` aufrufen und prüfen, dass nichts
     * geschrieben wird.
     */
    @Order(1)
    @Test
    public void testZero() throws IOException {
        var out = new ByteArrayOutputStream();
        IOTasks.writePowersOfTwo(out, 0);
        var text = out.toString(UTF_8).trim();
        assertEquals("", text);
    }

    /**
     * `writePowersOfTwo` mit `n = 1` aufrufen und prüfen, dass die Zahl 1
     * geschrieben wird.
     */
    @Order(2)
    @Test
    public void testOne() throws IOException {
        var out = new ByteArrayOutputStream();
        IOTasks.writePowersOfTwo(out, 1);
        var text = out.toString(UTF_8).trim();
        assertEquals("1", text);
    }

    /**
     * `writePowersOfTwo` mit verschiedenen `n` aufrufen und prüfen, dass die
     * ersten `n` Zweierpotenzen geschrieben werden.
     */
    @Order(3)
    @Test
    public void testMore() throws IOException {
        var out = new ByteArrayOutputStream();
        IOTasks.writePowersOfTwo(out, 4);
        var text = out.toString(UTF_8).trim(); // remove possible trailing \n
        assertEquals("""
                1
                2
                4
                8""", text);

        out = new ByteArrayOutputStream();
        IOTasks.writePowersOfTwo(out, 6);
        text = out.toString(UTF_8).trim();
        assertEquals("""
                1
                2
                4
                8
                16
                32""", text);

        out = new ByteArrayOutputStream();
        IOTasks.writePowersOfTwo(out, 13);
        text = out.toString(UTF_8).trim();
        assertEquals("""
                1
                2
                4
                8
                16
                32
                64
                128
                256
                512
                1024
                2048
                4096""", text);
    }

    /**
     * Prüfen, dass `writePowersOfTwo` den übergebenen OutputStream schliesst.
     */
    @Order(4)
    @Test
    public void testClose() throws IOException {
        var closed = new int[]{0};
        var out = new ByteArrayOutputStream() {
            public void close() {
                closed[0]++;
            }
        };
        IOTasks.writePowersOfTwo(out, 4);
        assertEquals(1, closed[0]);
    }
}
