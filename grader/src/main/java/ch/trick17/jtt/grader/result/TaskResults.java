package ch.trick17.jtt.grader.result;

import static java.util.EnumSet.noneOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.*;
import java.util.stream.Stream;

import ch.trick17.jtt.grader.Task;

/**
 * A container for the grading results of a whole {@link Task}, which is
 * basically a {@link Map} from submission names to {@link SubmissionResults}.
 */
public class TaskResults {

    private final Task task;
    private final Map<String, SubmissionResults> submissionResults = new HashMap<>();

    public TaskResults(Task task) {
        this.task = task;
    }

    public Task task() {
        return task;
    }

    /**
     * Returns the results for the given submission. This method can be safely
     * used from multiple threads, but the returned object cannot.
     */
    public synchronized SubmissionResults get(String submName) {
        return submissionResults.get(submName);
    }

    public synchronized void put(SubmissionResults res) {
        submissionResults.put(res.submissionName(), res);
    }

    /**
     * Returns a snapshot of the submission names.
     */
    public synchronized List<String> submissionNames() {
        return new ArrayList<>(submissionResults.keySet());
    }

    /**
     * Returns a snapshot of the submission results. This is a shallow copy,
     * i.e., the {@link SubmissionResults} objects are the same ones as those
     * returned by {@link #get(String)}.
     */
    public synchronized List<SubmissionResults> submissionResults() {
        return new ArrayList<>(submissionResults.values());
    }

    /**
     * Returns a snapshot of all criteria that are fulfilled by at least one
     * submission, grouped by kind.
     */
    public List<String> allCriteria() {
        var results = submissionResults(); // get snapshot
        var properties = results.stream()
                .flatMap(r -> r.properties().stream())
                .collect(toCollection(() -> noneOf(Property.class)));
        var tags = results.stream()
                .flatMap(r -> r.tags().stream())
                .collect(toCollection(TreeSet::new));
        var tests = results.stream()
                .flatMap(r -> r.passedTests().stream())
                .collect(toCollection(TreeSet::new));
        var streams = Stream.of(
                properties.stream().map(Property::prettyName),
                tags.stream(),
                tests.stream());
        return streams.flatMap(identity()).collect(toList());
    }
}
