package ch.trick17.jtt.grader;

import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ch.trick17.jtt.grader.Property.*;
import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;

public record GradeResult(
        String submissionName,
        boolean compileErrors,
        boolean testCompileErrors,
        boolean compiled,
        List<TestResult> testResults) {

    public List<Property> properties() {
        var withNull = Stream.of(
                compileErrors ? COMPILE_ERRORS : null,
                testCompileErrors ? TEST_COMPILE_ERRORS : null,
                compiled ? COMPILED : null,
                anyMatch(TestResult::nonDeterm) ? NONDETERMINISTIC : null,
                anyMatch(TestResult::timeout) ? TIMEOUT : null,
                anyMatch(TestResult::outOfMemory) ? OUT_OF_MEMORY : null,
                anyMatch(TestResult::incompleteReps) ? INCOMPLETE_REPETITIONS : null,
                anyMatch(m -> !m.illegalOps().isEmpty()) ? ILLEGAL_OPERATION : null);
        return withNull
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean anyMatch(Predicate<TestResult> predicate) {
        return compiled && testResults.stream().anyMatch(predicate);
    }

    /**
     * Tags are not supported right now, so this method returns an empty set.
     */
    public Set<String> tags() {
        return emptySet();
    }

    /**
     * Returns all tests that were executed for the submission, in the order of
     * execution. If no tests could be executed, the list is empty.
     */
    public List<TestMethod> allTests() {
        return Stream.ofNullable(testResults)
                .flatMap(List::stream)
                .map(TestResult::method)
                .toList();

    }

    public List<TestMethod> passedTests() {
        return Stream.ofNullable(testResults)
                .flatMap(List::stream)
                .filter(TestResult::passed)
                .map(TestResult::method)
                .toList();
    }

    public List<TestMethod> failedTests() {
        return Stream.ofNullable(testResults)
                .flatMap(List::stream)
                .filter(not(TestResult::passed))
                .map(TestResult::method)
                .toList();
    }

    public TestResult testResultFor(String testName) {
        return testResults.stream()
                .filter(r -> r.method().name().equals(testName))
                .findFirst()
                .orElseThrow();
    }
}
