package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.Codebase.Submission;
import ch.trick17.jtt.grader.result.SubmissionResults;
import ch.trick17.jtt.grader.result.TaskResults;
import ch.trick17.jtt.grader.result.TsvWriter;
import ch.trick17.jtt.grader.test.ForkedVmClient;
import ch.trick17.jtt.grader.test.TestExecutor;
import ch.trick17.jtt.grader.test.TestResults;
import ch.trick17.jtt.grader.test.TestRunConfig;
import org.apache.commons.io.output.TeeOutputStream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static java.io.File.pathSeparator;
import static java.io.Writer.nullWriter;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.stream;
import static java.util.Comparator.reverseOrder;
import static java.util.List.copyOf;
import static java.util.concurrent.ForkJoinPool.getCommonPoolParallelism;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.NOPOS;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.CLASS_PATH;

public class Grader implements Closeable {

    private static final Path GRADING_SRC = Path.of("src"); // all relative to grading dir
    private static final Path GRADING_TEST_SRC = Path.of("test-src");
    private static final Path GRADING_BIN = Path.of("bin");
    private static final Path GRADING_TEST_BIN = Path.of("test-bin");

    private static final Path ALL_RESULTS_FILE = Path.of("results-all.tsv").toAbsolutePath();
    private static final DateTimeFormatter LOG_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private Predicate<Submission> filter = p -> true;
    private int parallelism = getCommonPoolParallelism();
    private Path logDir = Path.of(".");
    private Path resultsDir = Path.of(".");
    private List<String> testVmArgs = List.of("-Dfile.encoding=UTF8");

    private ForkedVmClient testRunner;

    public void gradeOnly(String... submNames) {
        var set = new HashSet<>(List.of(submNames));
        filter = s -> set.contains(s.name());
    }

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

    public List<String> getTestVmArgs() {
        return testVmArgs;
    }

    /**
     * Sets the VM arguments that are used to start the JVM(s) in which the
     * tests are executed (in addition to predefined arguments such as the
     * classpath, which is equal to the one of this VM). The default is
     * "-Dfile.encoding=UTF8", so to enforce a different (or again the same)
     * encoding, a respective argument should be included when using this
     * method.
     */
    public void setTestVmArgs(String... testVmArgs) {
        this.testVmArgs = List.of(testVmArgs);
    }

    public List<TaskResults> run(Codebase codebase, List<Task> tasks) throws IOException {
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

        var submissions = codebase.submissions().stream()
                .filter(filter)
                .toList();

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
                        var res = gradeTask(subm, task, submOut);
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

    private SubmissionResults gradeTask(Submission subm, Task task, PrintStream out)
            throws IOException {
        return tryFinally(() -> {
            prepareProject(subm, task);
            var gradingDir = gradingDir(subm, task);

            var compileErrors = compile(task.compiler(),
                    gradingDir.resolve(GRADING_SRC),
                    gradingDir.resolve(GRADING_BIN),
                    task.dependencies(), out);

            var dependencies = new ArrayList<>(List.of(gradingDir.resolve(GRADING_BIN)));
            dependencies.addAll(task.dependencies());
            var testCompileErrors = compile(task.compiler(),
                    gradingDir.resolve(GRADING_TEST_SRC),
                    gradingDir.resolve(GRADING_TEST_BIN),
                    dependencies, out);

            var compiled = !compileErrors && !testCompileErrors
                           || task.compiler() == ECLIPSE;
            TestResults testResults = null;
            if (compiled) {
                testResults = runTests(task, subm, out);
            }
            return new SubmissionResults(subm.name(), compileErrors,
                    testCompileErrors, compiled, testResults);
        }, () -> {
            delete(gradingDir(subm, task), false);
        });
    }

    private void prepareProject(Submission subm, Task task) throws IOException {
        // Remove any grading files from previous runs
        var gradingDir = gradingDir(subm, task);
        delete(gradingDir, true);

        // create required dirs
        var srcDir = gradingDir.resolve(GRADING_SRC);
        var binDir = gradingDir.resolve(GRADING_BIN);
        var testSrcDir = gradingDir.resolve(GRADING_TEST_SRC);
        var testBinDir = gradingDir.resolve(GRADING_TEST_BIN);
        Files.createDirectories(srcDir);
        Files.createDirectories(binDir);
        Files.createDirectories(testSrcDir);
        Files.createDirectories(testBinDir);

        // Copy submission
        var origSrcDir = subm.srcDir();
        if (Files.isDirectory(origSrcDir)) { // yes, it happened
            try (var walk = Files.walk(origSrcDir)) {
                for (var from : (Iterable<Path>) walk::iterator) {
                    if (from.toString().endsWith(".java")) {
                        var to = srcDir.resolve(origSrcDir.relativize(from));
                        Files.createDirectories(to.getParent());
                        Files.copy(from, to);
                    }
                }
            }
        }

        // Copy given classes
        for (var entry : task.givenClasses().entrySet()) {
            var path = srcDir.resolve(entry.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue());
        }

        // Copy test classes
        for (var entry : task.testClasses().entrySet()) {
            var path = testSrcDir.resolve(entry.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue());
        }
    }

    private boolean compile(Compiler compiler, Path srcDir, Path outDir,
                            List<Path> dependencies, PrintStream out) throws IOException {
        Set<Path> sources;
        try (var walk = Files.walk(srcDir)) {
            sources = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(toSet());
        }
        if (sources.isEmpty()) {
            out.println("No sources found in " + srcDir);
            return false;
        }

        var classPath = stream(getProperty("java.class.path").split(pathSeparator))
                .map(File::new)
                .collect(toCollection(ArrayList::new));
        dependencies.forEach(p -> classPath.add(p.toFile()));

        var javaCompiler = compiler.create();
        var collector = new DiagnosticCollector<>();
        var manager = javaCompiler.getStandardFileManager(collector, null, UTF_8);
        manager.setLocation(CLASS_PATH, classPath);
        manager.setLocation(CLASS_OUTPUT, List.of(outDir.toFile()));

        var version = String.valueOf(Runtime.version().feature());
        var options = new ArrayList<>(List.of(
                "-source", version, "-target", version));
        if (compiler == ECLIPSE) {
            options.add("-proceedOnError");
        }
        javaCompiler.getTask(nullWriter(), manager, collector, options, null,
                manager.getJavaFileObjectsFromPaths(sources)).call();

        var errors = collector.getDiagnostics().stream()
                .filter(d -> d.getKind() == ERROR).toList();

        errors.forEach(d -> out.println(format(d, srcDir)));
        return !errors.isEmpty();
    }

    private TestResults runTests(Task task, Submission subm, PrintStream out) throws IOException {
        if (testRunner == null || !testRunner.getVmArgs().equals(testVmArgs)) {
            if (testRunner != null) {
                testRunner.close();
            }
            testRunner = new ForkedVmClient(testVmArgs);
        }

        var gradingDir = gradingDir(subm, task);
        var bin = gradingDir.resolve(GRADING_BIN);
        var testBin = gradingDir.resolve(GRADING_TEST_BIN);
        var config = new TestRunConfig(task.testClassName(), bin, testBin,
                task.repetitions(), task.repTimeout(), task.testTimeout(),
                task.permittedCalls(), task.dependencies());

        var results = testRunner.runInForkedVm(TestExecutor.class, "execute",
                List.of(config), TestResults.class);

        results.methodResults().forEach(res -> {
            res.failMsgs().stream()
                    .flatMap(s -> stream(s.split("\n")))
                    .map("    "::concat)
                    .forEach(out::println);
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
        });
        return results;
    }

    private Path gradingDir(Submission subm, Task task) {
        return subm.dir().resolve("grading-" + task.testClassName());
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

    private static void delete(Path dir, boolean leaveRoot) throws IOException {
        // create directory if it doesn't exist (needed for walk())
        Files.createDirectories(dir);
        try (var walk = Files.walk(dir)) {
            walk.skip(leaveRoot ? 1 : 0)
                    .map(Path::toFile)
                    .sorted(reverseOrder())
                    .forEach(File::delete);
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

    private String format(Diagnostic<?> problem, Path srcDir) {
        var path = Path.of(((JavaFileObject) problem.getSource()).toUri());
        return srcDir.relativize(path)
               + ":" + problem.getLineNumber()
               + ": " + problem.getKind()
               + ": " + problem.getMessage(null) + "\n"
               + formatSource(problem);
    }

    /**
     * Compiler-independent formatting of source location, based on
     * {@link
     * org.eclipse.jdt.internal.compiler.problem.DefaultProblem#errorReportSource(char[])}
     */
    private CharSequence formatSource(Diagnostic<?> problem) {
        char[] unitSource = null;
        try (var in = ((JavaFileObject) problem.getSource()).openInputStream()) {
            unitSource = new String(in.readAllBytes(), UTF_8).toCharArray();
        } catch (IOException ignored) {
        }

        var startPos = (int) problem.getStartPosition();
        var endPos = (int) problem.getEndPosition();
        int len;
        if ((startPos > endPos)
            || ((startPos == NOPOS) && (endPos == NOPOS))
            || (unitSource == null) || (len = unitSource.length) == 0) {
            return "";
        }

        char c;
        int start;
        int end;
        for (start = startPos >= len ? len - 1 : startPos; start > 0; start--) {
            if ((c = unitSource[start - 1]) == '\n' || c == '\r') {
                break;
            }
        }
        for (end = endPos >= len ? len - 1 : endPos; end + 1 < len; end++) {
            if ((c = unitSource[end + 1]) == '\r' || c == '\n') {
                break;
            }
        }

        // trim left and right spaces/tabs
        while ((c = unitSource[start]) == ' ' || c == '\t') {
            start++;
        }
        while ((c = unitSource[end]) == ' ' || c == '\t') {
            end--;
        }

        // copy source
        var result = new StringBuffer();
        result.append('\t').append(unitSource, start, end - start + 1);
        result.append("\n\t");

        // compute underline
        for (int i = start; i < startPos; i++) {
            result.append((unitSource[i] == '\t') ? '\t' : ' ');
        }
        result.append("^".repeat(Math.max(0, (endPos >= len ? len - 1 : endPos) - startPos + 1)));
        return result;
    }

    @Override
    public void close() {
        if (testRunner != null) {
            testRunner.close();
        }
    }
}
