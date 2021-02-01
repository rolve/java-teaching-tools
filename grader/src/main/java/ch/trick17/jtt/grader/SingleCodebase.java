package ch.trick17.jtt.grader;

import java.nio.file.Path;
import java.util.List;

public class SingleCodebase extends Codebase {

    private final Submission submission;

    public SingleCodebase(String name, Path dir, ProjectStructure structure) {
        super(structure);
        this.submission = new Submission(name, dir);
    }

    public List<Submission> submissions() {
        return List.of(submission);
    }
}
