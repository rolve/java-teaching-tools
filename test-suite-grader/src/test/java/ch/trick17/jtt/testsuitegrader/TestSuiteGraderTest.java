package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader.Submission;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static ch.trick17.jtt.testsuitegrader.RefCodeProvider.refImplementations;
import static ch.trick17.jtt.testsuitegrader.RefCodeProvider.refTestSuite;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestSuiteGraderTest {

    static TestSuiteGrader grader = new TestSuiteGrader();

    @ParameterizedTest
    @ValueSource(strings = {"adresse", "io-tasks", "phone-number", "random-sort", "smart-home"})
    void gradeRefTestSuite(String name) throws IOException {
        var refTestSuite = refTestSuite(name);
        var task = grader.prepareTask(refImplementations(name), refTestSuite);

        var result = grader.grade(task, new Submission(refTestSuite));

        // the reference test suite should let all reference implementations
        // pass and, by definition, none of the mutants
        assertTrue(result.compiled());
        assertFalse(result.emptyTestSuite());
        assertTrue(result.refImplementationResults().stream().allMatch(r -> r.passed()));
        assertTrue(result.mutantResults().stream().noneMatch(r -> r.passed()));
    }

    @Nested // @BeforeAll for a subset of tests
    public class GradeIncompleteTestSuite {

        final static String code = "jahreszeit";
        final static int totalTests = 8;

        static List<InMemSource> refTestSuite;
        static Task task;

        Pattern testPattern = Pattern.compile("""
                    @Order\\(\\d\\)
                    @Test
                    void \\w+\\(\\) \\{
                        [^}]+
                    }
                """);

        @BeforeAll
        static void setup() throws IOException {
            refTestSuite = refTestSuite(code);
            assumeTrue(refTestSuite.size() == 1);
            task = grader.prepareTask(refImplementations(code), refTestSuite);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
        void gradeIncompleteTestSuite(int missingTests) throws IOException {
            // leaving away tests from the "end" of the reference test suite
            // should result in surviving mutants and reduce the score in
            // proportion to the number of tests left away (given that each
            // test in the suite kills mutants not killed by earlier tests)
            var expectedScore = (totalTests - missingTests) / (double) totalTests;

            var testClass = refTestSuite.get(0).getContent();
            var matcher = testPattern.matcher(testClass);
            var matches = new ArrayList<MatchResult>();
            while (matcher.find()) {
                matches.add(matcher.toMatchResult());
            }
            assumeTrue(matches.size() == totalTests);

            var reducedTestClass = testClass;
            for (int i = 0; i < missingTests; i++) {
                var match = matches.get(totalTests - i - 1);
                reducedTestClass = reducedTestClass.substring(0, match.start()) +
                                   reducedTestClass.substring(match.end());
            }

            var testSuite = List.of(InMemSource.fromString(reducedTestClass));
            var result = grader.grade(task, new Submission(testSuite));

            assertTrue(result.refImplementationResults().stream().allMatch(r -> r.passed()));
            assertTrue(switch (missingTests) {
                case 0 -> result.mutantResults().stream().noneMatch(r -> r.passed());
                default -> result.mutantResults().stream().anyMatch(r -> r.passed()) &&
                           result.mutantResults().stream().anyMatch(r -> !r.passed());
                case totalTests -> result.mutantResults().stream().allMatch(r -> r.passed());
            });
            assertEquals(expectedScore, result.mutantScore(), 0.001);
        }
    }

    @Test
    void refTestDescriptions() throws IOException {
        var refTestSuite = refTestSuite("io-tasks");
        var task = grader.prepareTask(refImplementations("io-tasks"), refTestSuite);
        var desc = task.refTestDescriptions();

        assertEquals(desc.get(new TestMethod("io.FirstNonEmptyLinesTest", "testZero")),
                "<code>firstNonEmptyLines</code> mit <code>n = 0</code> aufrufen und prüfen, dass eine leere " +
                "Liste zurückgegeben wird.");
        assertEquals(desc.get(new TestMethod("io.FirstNonEmptyLinesTest", "testOne")),
                "<code>firstNonEmptyLines</code> mit <code>n = 1</code> aufrufen und prüfen, dass die erste " +
                "(nicht-leere) Zeile zurückgegeben wird.");
        assertEquals(desc.get(new TestMethod("io.FirstNonEmptyLinesTest", "testEncoding")),
                "<code>firstNonEmptyLines</code> mit einem Text aufrufen, der Nicht-ASCII-Zeichen " +
                "enthält, und prüfen, dass die Zeichen korrekt decodiert werden.");
        assertEquals(desc.get(new TestMethod("io.WritePowersOfTwoTest", "testClose")),
                "Prüfen, dass <code>writePowersOfTwo</code> den übergebenen OutputStream schliesst.");
    }

    @Test
    void convertTaskToJson() throws IOException {
        var mapper = new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(new TaskJacksonModule());
        var task = grader.prepareTask(refImplementations("jahreszeit"), refTestSuite("jahreszeit"));
        var string = mapper.writeValueAsString(task);
        var parsed = mapper.readValue(string, Task.class);
        assertEquals(task, parsed);
    }
}
