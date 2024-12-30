package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.Grader.Result;
import ch.trick17.jtt.grader.Grader.Task;
import ch.trick17.jtt.memcompile.InMemSource;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.join;
import static java.nio.file.Files.list;
import static java.time.LocalDateTime.now;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ForkJoinPool.getCommonPoolParallelism;
import static org.slf4j.LoggerFactory.getLogger;

public class BatchGrader implements Closeable {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final Logger logger = getLogger(BatchGrader.class);

    private final Path reportFile;
    private final Path resultsFile;
    private final int parallelism;

    private final Grader grader = new Grader();

    public BatchGrader() {
        this(Path.of("grader_report_" + now().format(TIME_FORMAT) + ".txt"),
                Path.of("grader_results_" + now().format(TIME_FORMAT) + ".tsv"));
    }

    public BatchGrader(Path reportFile, Path resultsFile) {
        this(reportFile, resultsFile, getCommonPoolParallelism());
    }

    public BatchGrader(Path reportFile, Path resultsFile, int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be positive");
        }
        this.reportFile = reportFile;
        this.resultsFile = resultsFile;
        this.parallelism = parallelism;
    }

    public void grade(Task task, List<Submission> submissions) throws IOException {
        grade(List.of(task), submissions);
    }

    public void grade(List<Task> tasks, List<Submission> submissions) throws IOException {
        var results = new LinkedHashMap<Task, Map<Submission, Result>>();
        tasks.forEach(t -> results.put(t, new ConcurrentHashMap<>()));

        var left = new AtomicInteger(submissions.size());
        try (var pool = new ForkJoinPool(parallelism)) {
            for (var subm : submissions) {
                pool.submit(() -> {
                    logger.info("Grading {}", subm.name);
                    try {
                        for (var task : tasks) {
                            if (tasks.size() > 1) {
                                logger.debug("Running task with tests {} for {}",
                                        join(", ", task.testClassNames()), subm.name);
                            }

                            var sources = Files.isDirectory(subm.srcDir)
                                    ? InMemSource.fromDirectory(subm.srcDir, null)
                                    : List.<InMemSource>of();
                            var res = grader.grade(task, sources);
                            results.get(task).put(subm, res);
                        }
                        logger.info("Finished grading {}, {} submissions left",
                                subm.name, left.decrementAndGet());
                    } catch (Throwable t) {
                        logger.error("Error while grading {}", subm.name, t);
                    }
                });
            }
        }

        if (reportFile != null) {
            ReportWriter.write(results, reportFile);
        }
        if (resultsFile != null) {
            TsvResultWriter.write(results, resultsFile);
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
         * directory. For example, given the following directory structure:
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
         * submissions, with names "bar", "baz", and "foo" (submissions are
         * sorted by name) and source directories
         * "/submissions/bar/src/main/java", "/submissions/baz/src/main/java",
         * and "/submissions/foo/src/main/java".
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
