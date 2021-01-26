package ch.trick17.jtt.grader;

import java.nio.file.Path;
import java.util.List;

public class SingleCodebase extends Codebase {

    private final Submission submission;

    public SingleCodebase(Path dir, ProjectStructure structure) {
        super(structure);
        this.submission = new Submission(dir.getFileName().toString(), dir);
    }

    public List<Submission> submissions() {
        return List.of(submission);
    }
}
