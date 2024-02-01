package ch.trick17.jtt.grader.result;

import ch.trick17.jtt.grader.Task;
import ch.trick17.jtt.testrunner.TestResults;
import ch.trick17.jtt.testrunner.TestResults.MethodResult;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ch.trick17.jtt.grader.result.Property.*;
import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

/**
 * A container for the grading results of a {@link Task} for a single
 * submission.
 */
public record SubmissionResults(
        String submissionName,
        boolean compileErrors,
        boolean testCompileErrors,
        boolean compiled,
        TestResults testResults) {

    public List<Property> properties() {
        var withNull = Stream.of(
                compileErrors ? COMPILE_ERRORS : null,
                testCompileErrors ? TEST_COMPILE_ERRORS : null,
                compiled ? COMPILED : null,
                anyMatch(MethodResult::nonDeterm) ? NONDETERMINISTIC : null,
                anyMatch(MethodResult::timeout) ? TIMEOUT : null,
                anyMatch(MethodResult::outOfMemory) ? OUT_OF_MEMORY : null,
                anyMatch(MethodResult::incompleteReps) ? INCOMPLETE_REPETITIONS : null,
                anyMatch(m -> !m.illegalOps().isEmpty()) ? ILLEGAL_OPERATION : null);
        return withNull
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private boolean anyMatch(Predicate<MethodResult> predicate) {
        return compiled && testResults.methodResults().stream().anyMatch(predicate);
    }

    /**
     * Tags are not supported right now, so this method returns an empty set.
     */
    public Set<String> tags() {
        return emptySet();
    }

    /**
     * Returns the names of all tests that were executed for the submission, in
     * the order of execution. If no tests could be executed, the list is
     * empty.
     */
    public List<String> allTests() {
        return Stream.ofNullable(testResults)
                .flatMap(r -> r.methodResults().stream())
                .map(MethodResult::method)
                .collect(toList());

    }

    public List<String> passedTests() {
        return Stream.ofNullable(testResults)
                .flatMap(r -> r.methodResults().stream())
                .filter(MethodResult::passed)
                .map(MethodResult::method)
                .collect(toList());
    }

    public List<String> failedTests() {
        return Stream.ofNullable(testResults)
                .flatMap(r -> r.methodResults().stream())
                .filter(not(MethodResult::passed))
                .map(MethodResult::method)
                .collect(toList());
    }
}
