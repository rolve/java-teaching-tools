package ch.trick17.jtt.grader.result;

import ch.trick17.jtt.grader.Task;
import ch.trick17.jtt.grader.test.TestResults;
import ch.trick17.jtt.grader.test.TestResults.MethodResult;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ch.trick17.jtt.grader.result.Property.*;
import static java.util.Collections.emptySet;
import static java.util.EnumSet.noneOf;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

/**
 * A container for the grading results of a {@link Task} for a single
 * submission.
 */
public class SubmissionResults {

    private final String submissionName;
    private final boolean compileErrors;
    private final boolean compiled;
    private final TestResults testResults;

    public SubmissionResults(String submissionName, boolean compileErrors,
                             boolean compiled, TestResults testResults) {
        this.submissionName = requireNonNull(submissionName);
        this.compileErrors = compileErrors;
        this.compiled = compiled;
        this.testResults = testResults;
    }

    public String submissionName() {
        return submissionName;
    }

    public boolean compileErrors() {
        return compileErrors;
    }

    public boolean compiled() {
        return compiled;
    }

    public TestResults testResults() {
        return testResults;
    }

    /**
     * The elements in the returned set are guaranteed to be sorted according to
     * their natural order.
     */
    public Set<Property> properties() {
        var withNull = Stream.of(
                compileErrors ? COMPILE_ERRORS : null,
                compiled ? COMPILED: null,
                anyMatch(MethodResult::nonDeterm) ? NONDETERMINISTIC: null,
                anyMatch(MethodResult::timeout) ? TIMEOUT : null,
                anyMatch(MethodResult::incompleteReps) ? INCOMPLETE_REPETITIONS : null,
                anyMatch(m -> !m.illegalOps().isEmpty()) ? ILLEGAL_OPERATION : null);
        return withNull
                .filter(Objects::nonNull)
                .collect(toCollection(() -> noneOf(Property.class)));
    }

    private boolean anyMatch(Predicate<MethodResult> predicate) {
        return compiled ? testResults.stream().anyMatch(predicate) : false;
    }

    /**
     * Tags are not supported right now, so this method returns an empty set.
     */
    public Set<String> tags() {
        return emptySet();
    }

    public Set<String> passedTests() {
        return Stream.ofNullable(testResults)
                .flatMap(TestResults::stream)
                .filter(MethodResult::passed)
                .map(MethodResult::method)
                .collect(toSet());
    }

    public Set<String> failedTests() {
        return Stream.ofNullable(testResults)
                .flatMap(TestResults::stream)
                .filter(not(MethodResult::passed))
                .map(MethodResult::method)
                .collect(toSet());
    }

    /**
     * Returns the criteria this submission fulfills. This set of criteria is
     * basically a simplified, string-based representation of this object.
     */
    public Set<String> criteria() {
        var streams = Stream.of(
                properties().stream().map(Property::prettyName),
                tags().stream(),
                passedTests().stream());
        return streams.flatMap(identity()).collect(toSet());
    }
}
