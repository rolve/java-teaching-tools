package io;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class ReadPeopleFromCsvTest {

    @Order(1)
    @Test
    public void testEmpty() throws IOException {
        var csv = """
                Name;Age;Positive
                """;
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals(emptyList(), people);
    }

    @Order(2)
    @Test
    public void testName() throws IOException {
        var csv = """
                Name;Age;Positive
                Maria Mopp;46;1
                Boris Bopp;23;0
                """;
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals("Maria Mopp", people.get(0).name());
        assertEquals("Boris Bopp", people.get(1).name());
    }

    @Order(3)
    @Test
    public void testAge() throws IOException {
        var csv = """
                Name;Age;Positive
                Maria Mopp;46;1
                Boris Bopp;23;0
                """;
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals(46, people.get(0).age());
        assertEquals(23, people.get(1).age());
    }

    @Order(4)
    @Test
    public void testPositive() throws IOException {
        var csv = """
                Name;Age;Positive
                Maria Mopp;46;1
                Boris Bopp;23;0
                """;
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertTrue(people.get(0).positive());
        assertFalse(people.get(1).positive());
    }

    @Order(5)
    @Test
    public void testEncoding() throws IOException {
        var csv = """
                Name;Age;Positive
                Boris Böpp;23;0
                Kuno «der Bulle» Koriander;78;1
                """;
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals("Boris Böpp", people.get(0).name());
        assertEquals("Kuno «der Bulle» Koriander", people.get(1).name());
    }

    @Order(6)
    @Test
    public void testClose() throws IOException {
        var csv = """
                Name;Age;Positive
                Maria Mopp;46;1
                Lisa Laufener;17;1
                """;
        var closed = new int[]{0};
        var in = new ByteArrayInputStream(csv.getBytes(UTF_8)) {
            public void close() {
                closed[0]++;
            }
        };
        IOTasks.readPeopleFromCsv(in);
        assertEquals(1, closed[0]);
    }

    private InputStream asStream(String text) {
        return new ByteArrayInputStream(text.getBytes(UTF_8));
    }
}
