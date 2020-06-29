package ch.trick17.jtt.grader.result;

import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.noneOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import ch.trick17.jtt.grader.Task;

/**
 * A container for the grading results of a {@link Task} for a single
 * submission.
 */
public class SubmissionResults {

    private final String submissionName;

    private final Set<Property> properties = noneOf(Property.class);
    private final Set<String> tags = new HashSet<>();
    private final Set<String> passedTests = new HashSet<>();

    public SubmissionResults(String submissionName) {
        this.submissionName = submissionName;
    }

    public String submissionName() {
        return submissionName;
    }

    /**
     * The elements in the returned set are guaranteed to be sorted according to
     * their natural order.
     */
    public Set<Property> properties() {
        return unmodifiableSet(properties);
    }

    public void addProperty(Property property) {
        properties.add(property);
    }

    public Set<String> tags() {
        return unmodifiableSet(tags);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public Set<String> passedTests() {
        return unmodifiableSet(passedTests);
    }

    public void addPassedTest(String test) {
        passedTests.add(test);
    }

    /**
     * Returns the criteria this submission fulfills. This set of criteria is
     * basically a simplified, string-based representation of this object.
     */
    public Set<String> criteria() {
        var streams = Stream.of(
                properties.stream().map(Property::prettyName),
                tags.stream(),
                passedTests.stream());
        return streams.flatMap(identity()).collect(toSet());
    }
}
