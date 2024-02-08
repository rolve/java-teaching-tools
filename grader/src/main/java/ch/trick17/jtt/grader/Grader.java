package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.result.SubmissionResults;
import ch.trick17.jtt.grader.result.TaskResults;
import ch.trick17.jtt.grader.result.TsvWriter;
import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.memcompile.InMemCompilation;
import ch.trick17.jtt.memcompile.InMemCompilation.Result;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testrunner.TestRunConfig;
import ch.trick17.jtt.testrunner.TestRunner;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static java.io.File.pathSeparator;
import static java.lang.String.join;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.List.copyOf;
import static java.util.concurrent.ForkJoinPool.getCommonPoolParallelism;
import static java.util.stream.Collectors.toCollection;

public class Grader implements Closeable {

    private static final Path ALL_RESULTS_FILE = Path.of("results-all.tsv").toAbsolutePath();
    private static final DateTimeFormatter LOG_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private int parallelism = getCommonPoolParallelism();
    private Path logDir = Path.of(".");
    private Path resultsDir = Path.of(".");

    private final TestRunner testRunner = new TestRunner();

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException();
        }
        this.parallelism = parallelism;
    }

    public Path getLogDir() {
        return logDir;
    }

    /**
     * Sets the directory in which log files are created. If <code>null</code>,
     * no log files are created. The default is the working directory (".").
     */
    public void setLogDir(Path logDir) {
        this.logDir = logDir;
    }

    public Path getResultsDir() {
        return resultsDir;
    }

    /**
     * Sets the directory in which result files are created. If
     * <code>null</code>, no result files are created. The default is the
     * working directory (".").
     */
    public void setResultsDir(Path resultsDir) {
        this.resultsDir = resultsDir;
    }

    public List<TaskResults> grade(List<Task> tasks, List<Submission> submissions) throws IOException {
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

        var results = new LinkedHashMap<Task, TaskResults>();
        tasks.forEach(t -> results.put(t, new TaskResults(t)));

        return tryFinally(() -> {
            var startTime = currentTimeMillis();
            var i = new AtomicInteger(0);

            BiConsumer<Submission, PrintStream> gradeSubm = (subm, submOut) -> {
                submOut.println("Grading " + subm.name());
                try {
                    for (var task : tasks) {
                        if (tasks.size() > 1) {
                            submOut.println(task.testClassName());
                        }
                        var res = grade(task, subm, submOut);
                        results.get(task).put(res);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
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

            return copyOf(results.values());
        }, () -> {
            if (resultsDir != null) {
                writeResultsToFile(results.values());
            }
            if (log != null) {
                log.close();
            }
        });
    }

    private SubmissionResults grade(Task task, Submission subm, PrintStream out)
            throws IOException {
        var sources = collectSources(subm.srcDir());
        for (var source : task.givenSources()) {
            sources.removeIf(s -> s.getPath().equals(source.getPath()));
            sources.add(source);
        }

        // compile submission
        Result compileResult;
        if (sources.isEmpty()) {
            compileResult = new Result(false, emptyList());
        } else {
            compileResult = InMemCompilation.compile(task.compiler(), sources,
                    ClassPath.fromFiles(task.dependencies()), out);
        }

        // compile tests
        var fileClassPath = stream(getProperty("java.class.path").split(pathSeparator))
                .map(Path::of)
                .collect(toCollection(ArrayList::new));
        fileClassPath.addAll(task.dependencies());
        var testCompileResult = InMemCompilation.compile(task.compiler(),
                task.testSources(), new ClassPath(compileResult.output(), fileClassPath), out);

        // run tests
        var compiled = !testCompileResult.output().isEmpty();
        List<TestResult> testResults = null;
        if (compiled) {
            testResults = runTests(task, compileResult.output(),
                    testCompileResult.output(), out);
        }
        return new SubmissionResults(subm.name(), compileResult.errors(),
                testCompileResult.errors(), compiled, testResults);
    }

    private static List<InMemSource> collectSources(Path srcDir) throws IOException {
        if (!Files.isDirectory(srcDir)) {
            return emptyList();
        }
        var sources = new ArrayList<InMemSource>();
        try (var javaFiles = Files.walk(srcDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))) {
            for (var file : (Iterable<Path>) javaFiles::iterator) {
                sources.add(InMemSource.fromFile(file, srcDir));
            }
        }
        return sources;
    }

    private List<TestResult> runTests(Task task,
                                 List<InMemClassFile> classes,
                                 List<InMemClassFile> testClasses,
                                 PrintStream out) throws IOException {
        var supportCode = ClassPath.fromMemory(testClasses)
                .withFiles(task.dependencies())
                .withCurrent();
        var config = new TestRunConfig(List.of(task.testClassName()),
                ClassPath.fromMemory(classes), supportCode,
                task.repetitions(), task.repTimeout(), task.testTimeout(),
                task.permittedCalls(), task.testVmArgs());

        var results = testRunner.run(config);

        for (var res : results) {
            for (var e : res.exceptions()) {
                out.printf("    %s (%s)\n",
                        valueOf(e.getMessage()).replaceAll("\\s+", " "),
                        e.getClass().getName());
            }
            if (res.nonDeterm()) {
                out.println("Non-determinism in " + res.method());
            }
            if (res.incompleteReps()) {
                out.println("Only " + res.repsMade() + " repetitions made in " + res.method());
            }
            if (res.timeout()) {
                out.println("Timeout in " + res.method());
            }
            if (res.outOfMemory()) {
                out.println("Out of memory in " + res.method());
            }
            if (!res.illegalOps().isEmpty()) {
                out.println("Illegal operation(s) in " + res.method() + ": " +
                            join(", ", res.illegalOps()));
            }
        }
        return results;
    }

    private void writeResultsToFile(Collection<TaskResults> results) throws IOException {
        for (var r : results) {
            var filename = "results-" + r.task().testClassSimpleName() + ".tsv";
            TsvWriter.write(List.of(r), resultsDir.resolve(filename));
        }
        if (results.size() > 1) {
            TsvWriter.write(results, resultsDir.resolve(ALL_RESULTS_FILE));
        }
    }

    /**
     * Like a normal try-finally, but with the superior exception handling of a
     * try-with-resources, i.e., does not suppress exceptions thrown from the
     * try block. Note that {@link Closeable} is used simply as a functional
     * interface here.
     */
    private static <T> T tryFinally(CallableIO<T> tryBlock, Closeable finallyBlock)
            throws IOException {
        try (finallyBlock) {
            return tryBlock.call();
        }
    }

    private interface CallableIO<T> {
        T call() throws IOException;
    }

    @Override
    public void close() {
        testRunner.close();
    }
}
