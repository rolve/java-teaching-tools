package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.jtt.testrunner.TestRunnerJacksonModule;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void builtInException() {
        try (var client = new ForkedVmClient(emptyList(), List.of(TestRunnerJacksonModule.class))) {
            var e = assertThrows(IllegalArgumentException.class, () -> {
                client.runInForkedVm(TestCode.class, "greeting4",
                        List.of("not a number"), String.class);
            });
            assertEquals("'number' is not an int", e.getMessage());
            assertEquals("greeting4", e.getStackTrace()[0].getMethodName());
            assertEquals(TestCode.class.getName(), e.getStackTrace()[0].getClassName());
            assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }

    @Test
    void assertionFailedError() {
        try (var client = new ForkedVmClient(emptyList(), List.of(TestRunnerJacksonModule.class))) {
            var e = assertThrows(AssertionFailedError.class, () -> {
                client.runInForkedVm(TestCode.class, "greeting5", emptyList(), Void.class);
            });
            assertEquals("expected: <3> but was: <2>", e.getMessage());
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

        public static String greeting4(String number) {
            try {
                return "Hello".repeat(Integer.parseInt(number));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'number' is not an int", e);
            }
        }

        public static void greeting5() {
            assertEquals(3, 1 + 1);
        }
    }

    public static class Person {
        public String name;
    }
}
