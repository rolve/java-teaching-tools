package ch.trick17.jtt.grader.result;

import ch.trick17.jtt.grader.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
