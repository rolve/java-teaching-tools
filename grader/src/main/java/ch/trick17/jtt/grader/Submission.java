package ch.trick17.jtt.grader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.list;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

public record Submission(String name, Path srcDir) {

    public Submission {
        name = requireNonNull(name);
        srcDir = srcDir.toAbsolutePath().normalize();
    }

    /**
     * Loads multiple submissions from a <code>root</code> directory. Each
     * direct subdirectory in that directory is considered a submission. The
     * source directory of each submission is determined by resolving
     * <code>srcDir</code> (a relative path) against the submission directory.
     * For example, given the following directory structure:
     * <pre>
     * /
     *     submissions/
     *         foo/
     *             src/main/java/
     *         bar/
     *             src/main/java/
     *         baz/
     *             src/main/java/
     * </pre>
     * calling this method with "/submissions" as <code>root</code> and
     * "src/main/java" as <code>srcDir</code>, this method would return a list
     * containing three submissions, with names "bar", "baz", and "foo"
     * (submissions are sorted by name) and source directories
     * "/submissions/bar/src/main/java", "/submissions/baz/src/main/java",
     * and "/submissions/foo/src/main/java".
     */
    public static List<Submission> loadAllFrom(Path root, Path srcDir) throws IOException {
        if (srcDir.isAbsolute()) {
            throw new IllegalArgumentException("srcDir must be a relative path");
        }
        try (var list = list(root)) {
            return list
                    .filter(Files::isDirectory)
                    .map(dir -> new Submission(dir.getFileName().toString(), dir.resolve(srcDir)))
                    .sorted(comparing(Submission::name))
                    .toList();
        }
    }
}
