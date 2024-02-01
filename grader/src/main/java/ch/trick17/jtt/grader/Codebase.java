package ch.trick17.jtt.grader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;

public abstract class Codebase {

    private final ProjectStructure structure;

    public Codebase(ProjectStructure structure) {
        this.structure = requireNonNull(structure);
    }

    public abstract List<Submission> submissions() throws IOException;

    public class Submission implements Comparable<Submission> {

        private final Path dir;
        private final String name;

        public Submission(String name, Path dir) {
            this.name = name;
            this.dir = dir;
        }

        public String name() {
            return name;
        }

        public Path dir() {
            return dir.normalize();
        }

        public Path srcDir() {
            return dir().resolve(structure.srcDir);
        }

        @Override
        public int compareTo(Submission other) {
            return dir.compareTo(other.dir);
        }
    }
}
