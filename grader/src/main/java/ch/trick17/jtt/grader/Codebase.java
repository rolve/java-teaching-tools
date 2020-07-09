package ch.trick17.jtt.grader;

import static java.nio.file.Files.list;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Codebase {

    private final Path root;
    private final Path subdir; // relative to submission dir
    private final ProjectStructure structure;

    public Codebase(Path root, ProjectStructure structure) {
        this(root, Path.of(""), structure);
    }

    public Codebase(Path root, Path subdir, ProjectStructure structure) {
        // make root path absolute, to avoid problems with relativize() later
        this.root = root.toAbsolutePath();
        this.subdir = requireNonNull(subdir);
        if (subdir.isAbsolute()) {
            throw new IllegalArgumentException("subdirectory must be a relative path");
        }
        this.structure = requireNonNull(structure);
    }

    public Stream<Submission> submissions() throws IOException {
        return list(root)
                .filter(Files::isDirectory)
                .map(Submission::new)
                .sorted();
    }

    public class Submission implements Comparable<Submission> {

        private final Path dir;

        public Submission(Path dir) {
            this.dir = dir;
        }

        public String name() {
            return dir.getFileName().toString();
        }

        public Path projectDir() {
            return dir.resolve(subdir).normalize();
        }

        public Path srcDir() {
            return projectDir().resolve(structure.srcDir);
        }

        @Override
        public int compareTo(Submission other) {
            return dir.compareTo(other.dir);
        }
    }
}
