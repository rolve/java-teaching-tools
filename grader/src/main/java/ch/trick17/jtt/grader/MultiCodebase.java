package ch.trick17.jtt.grader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.list;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class MultiCodebase extends Codebase {

    private final Path root;
    private final Path subdir; // relative to submission dir

    public MultiCodebase(Path root, ProjectStructure structure) {
        this(root, Path.of(""), structure);
    }

    public MultiCodebase(Path root, Path subdir, ProjectStructure structure) {
        super(structure);
        // make root path absolute, to avoid problems with relativize() later
        this.root = root.toAbsolutePath();
        this.subdir = requireNonNull(subdir);
        if (subdir.isAbsolute()) {
            throw new IllegalArgumentException("subdirectory must be a relative path");
        }
    }

    public List<Submission> submissions() throws IOException {
        try (var list = list(root)) {
            return list
                    .filter(Files::isDirectory)
                    .map(dir -> new Submission(dir.getFileName().toString(), dir.resolve(subdir)))
                    .sorted()
                    .collect(toList());
        }
    }
}
