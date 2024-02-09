package ch.trick17.jtt.testsuitegrader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static ch.trick17.jtt.testsuitegrader.RefCodeProvider.refImplementations;
import static ch.trick17.jtt.testsuitegrader.RefCodeProvider.refTestSuite;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuiteGraderTest {

    @ParameterizedTest
    @ValueSource(strings = {"adresse", "jahreszeit", "phone-number", "random-sort", "smart-home"})
    void gradeRefTestSuite(String name) throws IOException {
        var refTestSuite = refTestSuite(name);
        var grader = new TestSuiteGrader();
        var task = grader.prepareTask(refImplementations(name), refTestSuite);

        var result = grader.grade(task, new Submission(refTestSuite));

        // the reference test suite should let all reference implementations
        // pass and, by definition, none of the mutants
        assertTrue(result.compiled());
        assertFalse(result.emptyTestSuite());
        assertTrue(result.refImplementationResults().stream().allMatch(r -> r.passed()));
        assertTrue(result.mutantResults().stream().noneMatch(r -> r.passed()));
    }
}
