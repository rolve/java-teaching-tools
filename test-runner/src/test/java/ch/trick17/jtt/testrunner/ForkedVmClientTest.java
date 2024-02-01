package ch.trick17.jtt.testrunner;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForkedVmClientTest {

    @Test
    void noArgs() throws IOException {
        try (var client = new ForkedVmClient()) {
            var result = client.runInForkedVm(TestCode.class, "greeting1",
                    emptyList(), String.class);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    void standardArgs() throws IOException {
        try (var client = new ForkedVmClient()) {
            var result = client.runInForkedVm(TestCode.class, "greeting2",
                    List.of("Michael", 3), String.class);
            assertEquals("Hello, Michael!!!", result);
        }
    }

    @Test
    void customClassesArgs() throws IOException {
        try (var client = new ForkedVmClient()) {
            var person = new Person();
            person.name = "Michael";
            var result = client.runInForkedVm(TestCode.class, "greeting3",
                    List.of(person), String.class);
            assertEquals("Hello, Michael!", result);
        }
    }

    public static class TestCode {
        public static String greeting1() {
            return "Hello, World!";
        }

        public static String greeting2(String name, int exclamationMarks) {
            return "Hello, " + name + "!".repeat(exclamationMarks);
        }

        public static String greeting3(Person person) {
            return "Hello, " + person.name + "!";
        }
    }

    public static class Person {
        public String name;
    }
}
