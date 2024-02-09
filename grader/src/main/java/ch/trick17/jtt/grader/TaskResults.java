package ch.trick17.jtt.grader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A container for the grading results of a whole {@link Task}, which is
 * basically a {@link Map} from submission names to {@link GradeResult}.
 */
public class TaskResults {

    private final Task task;
    private final Map<String, GradeResult> submissionResults = new HashMap<>();

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
    public synchronized GradeResult get(String submName) {
        return submissionResults.get(submName);
    }

    public synchronized void put(GradeResult res) {
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
     * i.e., the {@link GradeResult} objects are the same ones as those
     * returned by {@link #get(String)}.
     */
    public synchronized List<GradeResult> submissionResults() {
        return new ArrayList<>(submissionResults.values());
    }
}
