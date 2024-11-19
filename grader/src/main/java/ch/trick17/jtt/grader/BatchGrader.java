package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.Grader.Result;
import ch.trick17.jtt.grader.Grader.Task;
import ch.trick17.jtt.memcompile.InMemSource;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.list;
import static java.time.LocalDateTime.now;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ForkJoinPool.getCommonPoolParallelism;

public class BatchGrader implements Closeable {

    private static final Path DEFAULT_DIR = Path.of(".");
    public static final Path RESULTS_FILE = Path.of("results.tsv").toAbsolutePath();

    private static final DateTimeFormatter LOG_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path logDir;
    private final Path resultsDir;
    private final int parallelism;

    private final Grader grader = new Grader();

    public BatchGrader() {
        this(DEFAULT_DIR, DEFAULT_DIR);
    }

    public BatchGrader(Path logDir, Path resultsDir) {
        this(logDir, resultsDir, getCommonPoolParallelism());
    }

    public BatchGrader(Path logDir, Path resultsDir, int parallelism) {
        this.logDir = logDir;
        this.resultsDir = resultsDir;
        this.parallelism = parallelism;
    }

    public void grade(Task task, List<Submission> submissions) throws IOException {
        grade(List.of(task), submissions);
    }

    public void grade(List<Task> tasks, List<Submission> submissions) throws IOException {
        OutputStream log;
        PrintStream out;
        if (logDir != null) {
            var logFile = "grader_" + now().format(LOG_FORMAT) + ".log";
            log = Files.newOutputStream(logDir.resolve(logFile));
            out = new PrintStream(new TeeOutputStream(System.out, log), true);
        } else {
            log = null;
            out = System.out;
        }

        var results = new LinkedHashMap<Task, Map<Submission, Result>>();
        tasks.forEach(t -> results.put(t, new ConcurrentHashMap<>()));

        tryFinally(() -> {
            var startTime = currentTimeMillis();
            var i = new AtomicInteger(0);

            BiConsumer<Submission, PrintStream> gradeSubm = (subm, submOut) -> {
                submOut.println("Grading " + subm.name);
                try {
                    for (var task : tasks) {
                        if (tasks.size() > 1) {
                            submOut.println(join(", ", task.testClassNames()));
                        }

                        var sources = Files.isDirectory(subm.srcDir)
                                ? InMemSource.fromDirectory(subm.srcDir, null)
                                : List.<InMemSource>of();
                        var res = grader.grade(task, sources, submOut);
                        results.get(task).put(subm, res);
                    }
                } catch (Throwable t) {
                    throw new RuntimeException("while grading " + subm.name, t);
                }
                submOut.printf("Graded %d/%d, total time: %d s\n\n",
                        i.incrementAndGet(), submissions.size(),
                        (currentTimeMillis() - startTime) / 1000);
            };

            if (parallelism == 1 || submissions.size() == 1) {
                submissions.forEach(subm -> gradeSubm.accept(subm, out));
            } else {
                new ForkJoinPool(parallelism).submit(() -> {
                    submissions.parallelStream().forEach(subm -> {
                        // need to buffer output for each submission, to avoid
                        // interleaving of lines
                        var buffer = new ByteArrayOutputStream();
                        gradeSubm.accept(subm, new PrintStream(buffer));
                        out.println(buffer);
                    });
                }).join();
            }

            out.println(submissions.size() + " submissions graded");
        }, () -> {
            if (resultsDir != null) {
                TsvWriter.write(results, resultsDir.resolve(RESULTS_FILE));
            }
            if (log != null) {
                log.close();
            }
        });
    }

    /**
     * Like a normal try-finally, but with the superior exception handling of a
     * try-with-resources, i.e., does not suppress exceptions thrown from the
     * try block. Note that {@link Closeable} is used simply as a functional
     * interface here (i.e. a Runnable that can throw IOException)
     */
    private void tryFinally(Closeable tryBlock, Closeable finallyBlock)
            throws IOException {
        try (finallyBlock) {
            tryBlock.close();
        }
    }

    @Override
    public void close() {
        grader.close();
    }

    public record Submission(String name, Path srcDir) {

        public Submission {
            name = requireNonNull(name);
            srcDir = srcDir.toAbsolutePath().normalize();
        }

        /**
         * Loads multiple submissions from a <code>root</code> directory, as
         * described in {@link Submission#loadAllFrom(Path, Path, boolean)}.
         * Hidden directories (i.e., directories whose name starts with a dot)
         * are ignored.
         */
        public static List<Submission> loadAllFrom(Path root, Path srcDir) throws IOException {
            return loadAllFrom(root, srcDir, true);
        }

        /**
         * Loads multiple submissions from a <code>root</code> directory. Each
         * direct subdirectory in that directory is considered a submission. The
         * source directory of each submission is determined by resolving
         * <code>srcDir</code> (a relative path) against the submission
         * directory.
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
         * "src/main/java" as
         * <code>srcDir</code>, this method would return a list containing three
         * submissions, with
         * names "bar", "baz", and "foo" (submissions are sorted by name) and
         * source directories "/submissions/bar/src/main/java",
         * "/submissions/baz/src/main/java", and
         * "/submissions/foo/src/main/java".
         * <p>
         * If <code>ignoreHidden</code> is <code>true</code>, hidden directories
         * (i.e., directories whose name starts with a dot) are ignored.
         */
        public static List<Submission> loadAllFrom(Path root, Path srcDir,
                                                   boolean ignoreHidden) throws IOException {
            if (srcDir.isAbsolute()) {
                throw new IllegalArgumentException("srcDir must be a relative path");
            }
            try (var list = list(root)) {
                return list
                        .filter(Files::isDirectory)
                        .filter(dir -> (!ignoreHidden || !dir.getFileName().toString().startsWith(".")))
                        .map(dir -> new Submission(dir.getFileName().toString(), dir.resolve(srcDir)))
                        .sorted(comparing(Submission::name))
                        .toList();
            }
        }
    }
}
