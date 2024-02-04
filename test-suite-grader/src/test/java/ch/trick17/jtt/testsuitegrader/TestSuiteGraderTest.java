package ch.trick17.jtt.testsuitegrader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static ch.trick17.jtt.testsuitegrader.RefCodeProvider.refImplementations;
import static ch.trick17.jtt.testsuitegrader.RefCodeProvider.refTestSuite;

public class TestSuiteGraderTest {

    @ParameterizedTest
    @ValueSource(strings = {"adresse", "jahreszeit", "phone-number", "random-sort"})
    void grade(String name) throws IOException {
        var task = new Task(refImplementations(name), refTestSuite(name));
        var grader = new TestSuiteGrader();
        grader.grade(null, task);
    }
}
