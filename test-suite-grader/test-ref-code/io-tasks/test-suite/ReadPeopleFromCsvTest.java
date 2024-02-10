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
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(OrderAnnotation.class)
public class ReadPeopleFromCsvTest {

    @Order(1)
    @Test
    public void testBasic() throws IOException {
        var csv = """
                Name;Age;Positive
                Maria Mopp;46;1
                Boris Bopp;23;0
                Sarah Sutter;39;0
                Kuno Koriander;78;1
                Lisa Laufener;17;1
                """;
        var expected = List.of(
                new Person("Maria Mopp", 46, true),
                new Person("Boris Bopp", 23, false),
                new Person("Sarah Sutter", 39, false),
                new Person("Kuno Koriander", 78, true),
                new Person("Lisa Laufener", 17, true));
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals(expected, people);
    }

    @Order(2)
    @Test
    public void testEmpty() throws IOException {
        var csv = """
                Name;Age;Positive
                """;
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals(emptyList(), people);
    }

    @Order(3)
    @Test
    public void testEncoding() throws IOException {
        var csv = """
                Name;Age;Positive
                Boris Böpp;23;0
                Sarah Sutter;39;0
                Kuno «der Bulle» Koriander;78;1
                大伴 弟麻呂;30;1
                """;
        var expected = List.of(
                new Person("Boris Böpp", 23, false),
                new Person("Sarah Sutter", 39, false),
                new Person("Kuno «der Bulle» Koriander", 78, true),
                new Person("大伴 弟麻呂", 30, true));
        var people = IOTasks.readPeopleFromCsv(asStream(csv));
        assertEquals(expected, people);
    }

    @Order(4)
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
