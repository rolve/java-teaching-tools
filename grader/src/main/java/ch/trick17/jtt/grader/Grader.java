package ch.trick17.jtt.grader;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static ch.trick17.jtt.grader.TestRunner.*;
import static ch.trick17.jtt.grader.result.Property.COMPILED;
import static ch.trick17.jtt.grader.result.Property.COMPILE_ERRORS;
import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.io.Writer.nullWriter;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.time.LocalDateTime.now;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ForkJoinPool.getCommonPoolParallelism;
import static java.util.stream.Collectors.*;
import static javax.tools.Diagnostic.NOPOS;
import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.*;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.tools.*;

import org.apache.commons.io.output.TeeOutputStream;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;
import ch.trick17.jtt.grader.Codebase.Submission;
import ch.trick17.jtt.grader.result.*;

public class Grader {

    private static final Path GRADING_SRC = Path.of("src"); // relative to grading dir
    private static final Path GRADING_BIN = Path.of("bin"); // relative to grading dir
    private static final Path DEFAULT_TESTS_DIR = Path.of("tests").toAbsolutePath();
    private static final Path ALL_RESULTS_FILE = Path.of("results-all.tsv").toAbsolutePath();
    private static final DateTimeFormatter LOG_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Codebase codebase;
    private final List<Task> tasks;

    private final Map<Task, TaskResults> results = new LinkedHashMap<>();
    private Predicate<Submission> filter = p -> true;
    private Path testsDir = DEFAULT_TESTS_DIR;
    private int parallelism = getCommonPoolParallelism();
    private Path codeTagsAgent;

    public Grader(Codebase codebase, List<Task> tasks) {
        this.codebase = codebase;
        this.tasks = requireNonNull(tasks);
        tasks.forEach(t -> results.put(t, new TaskResults(t)));
    }

    public void setTestsDir(Path testsDir) {
        this.testsDir = testsDir.toAbsolutePath();
    }

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

    public void run() throws IOException {
        var logFile = Path.of("grader_" + now().format(LOG_FORMAT) + ".log");
        var log = Files.newOutputStream(logFile);
        var out = new PrintStream(new TeeOutputStream(System.out, log), true);

        List<Submission> submissions;
        try (var all = codebase.submissions()) {
            submissions = all.filter(filter).collect(toList());
        }

        codeTagsAgent = Files.createTempFile("code-tags", ".jar");
        tryFinally(() -> {
            try (var in = Grader.class.getResourceAsStream("/code-tags.jar")) {
                Files.copy(in, codeTagsAgent, REPLACE_EXISTING);
            }

            var startTime = currentTimeMillis();
            var i = new AtomicInteger(0);

            BiConsumer<Submission, PrintStream> gradeSubm = (subm, submOut) -> {
                submOut.println("Grading " + subm.name());
                try {
                    for (var task : tasks) {
                        gradeTask(subm, task, submOut);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                submOut.printf("Graded %d/%d, total time: %d s\n\n",
                        i.incrementAndGet(), submissions.size(),
                        (currentTimeMillis() - startTime) / 1000);
            };

            if (parallelism == 1) {
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
            writeResultsToFile();
            Files.delete(codeTagsAgent);
            log.close();
        });
    }

    private void gradeTask(Submission subm, Task task, PrintStream out)
            throws IOException {
        tryFinally(() -> {
            prepareProject(subm, task);
            var hasErrors = compile(subm, task, out);
            if (hasErrors) {
                results.get(task).get(subm.name()).addProperty(COMPILE_ERRORS);
            }
            if (!hasErrors || task.compiler() == ECLIPSE) {
                results.get(task).get(subm.name()).addProperty(COMPILED);
                runTests(task, subm, out);
            }
        }, () -> {
            delete(gradingDir(subm, task), false);
        });
    }

    private void prepareProject(Submission subm, Task task) throws IOException {
        // Remove any grading files from previous runs
        var gradingDir = gradingDir(subm, task);
        delete(gradingDir, true);

        // Copy sources
        var origSrcDir = subm.srcDir();
        Files.createDirectories(origSrcDir); // yes, it happened
        var srcDir = gradingDir.resolve(GRADING_SRC);
        Files.createDirectories(srcDir);
        try (var walk = Files.walk(origSrcDir)) {
            for (var from : (Iterable<Path>) walk::iterator) {
                if (from.toString().endsWith(".java")) {
                    var to = srcDir.resolve(origSrcDir.relativize(from));
                    Files.createDirectories(to.getParent());
                    Files.copy(from, to);
                }
            }
        }

        // Copy grading files
        for (var file : task.filesToCopy()) {
            var from = testsDir.resolve(file);
            var to = srcDir.resolve(file);
            Files.createDirectories(to.getParent());
            Files.copy(from, to, REPLACE_EXISTING);
        }

        // Copy properties files into bin directory
        var binDir = gradingDir.resolve(GRADING_BIN);
        Files.createDirectories(binDir);
        try (var walk = Files.walk(origSrcDir)) {
            for (var from : (Iterable<Path>) walk::iterator) {
                if (from.toString().endsWith(".properties")) {
                    var to = binDir.resolve(origSrcDir.relativize(from));
                    Files.createDirectories(to.getParent());
                    Files.copy(from, to);
                }
            }
        }
    }

    private boolean compile(Submission subm, Task task, PrintStream out) throws IOException {
        var gradingDir = gradingDir(subm, task);
        var srcDir = gradingDir.resolve(GRADING_SRC);

        Set<Path> sources;
        try (var walk = Files.walk(srcDir)) {
            sources = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(toSet());
        }

        var javaCompiler = task.compiler().create();

        var collector = new DiagnosticCollector<>();
        var manager = javaCompiler.getStandardFileManager(collector, null, UTF_8);

        var version = String.valueOf(Runtime.version().feature());
        var classPath = getProperty("java.class.path") + pathSeparator + codeTagsAgent;
        var options = new ArrayList<>(List.of(
                "-cp", classPath,
                "-d", gradingDir.resolve(GRADING_BIN).toString(),
                "-source", version, "-target", version));
        if (task.compiler() == ECLIPSE) {
            options.add("-proceedOnError");
        }
        javaCompiler.getTask(nullWriter(), manager, collector, options, null,
                manager.getJavaFileObjectsFromPaths(sources)).call();

        var errors = collector.getDiagnostics().stream()
                .filter(d -> d.getKind() == ERROR).collect(toList());

        errors.forEach(d -> out.println(format(d, srcDir)));
        return errors.size() > 0;
    }

    private Path gradingDir(Submission subm, Task task) {
        return subm.projectDir().resolve("grading-" + task.testClass());
    }

    private void runTests(Task task, Submission subm, PrintStream out) throws IOException {
        var gradingDir = gradingDir(subm, task);
        var agentArg = "-javaagent:" + codeTagsAgent + "="
                + classesToInspect(gradingDir.resolve(GRADING_SRC));

        var jUnit = new JavaProcessBuilder(TestRunner.class, task.testClass())
                .addClasspath(gradingDir.resolve(GRADING_BIN).toString())
                .vmArgs(agentArg,
                        "-XX:-OmitStackTraceInFastThrow",
                        "-Dfile.encoding=UTF8",
                        "-D" + REPETITIONS_PROP + "=" + task.repetitions(),
                        "-D" + REP_TIMEOUT_PROP + "=" + task.repTimeout().toMillis(),
                        "-D" + TEST_TIMEOUT_PROP + "=" + task.testTimeout().toMillis())
                .start();

        var jUnitOutput = new StringWriter();
        var outCopier = new Thread(new LineCopier(jUnit.getInputStream(),
                new LineWriterAdapter(jUnitOutput)));
        var errCopier = new Thread(new LineCopier(jUnit.getErrorStream(),
                new LineWriterAdapter(out)));
        outCopier.start();
        errCopier.start();

        while (true) {
            try {
                outCopier.join();
                errCopier.join();
                break;
            } catch (InterruptedException e) {}
        }
        jUnitOutput.toString().lines()
                .filter(line -> line.startsWith("prop: "))
                .map(line -> Property.valueOf(line.substring(6)))
                .forEach(results.get(task).get(subm.name())::addProperty);
        jUnitOutput.toString().lines()
                .filter(line -> line.startsWith("tag: "))
                .map(line -> line.substring(5))
                .forEach(results.get(task).get(subm.name())::addTag);
        jUnitOutput.toString().lines()
                .filter(line -> line.startsWith("test: "))
                .map(line -> line.substring(6))
                .forEach(results.get(task).get(subm.name())::addPassedTest);
    }

    private void writeResultsToFile() throws IOException {
        for (var e : results.entrySet()) {
            TsvWriter.write(List.of(e.getValue()), e.getKey().resultFile());
        }
        if (tasks.size() > 1) {
            TsvWriter.write(results.values(), ALL_RESULTS_FILE);
        }
    }

    private static String classesToInspect(Path srcDir) throws IOException {
        try (var all = Files.walk(srcDir)) {
            return all
                    .map(p -> srcDir.relativize(p).toString())
                    .filter(s -> s.endsWith(".java"))
                    .map(s -> s.substring(0, s.length() - 5))
                    .map(s -> s.replace(separatorChar, '.'))
                    .collect(joining(","));
        }
    }

    private static void delete(Path dir, boolean leaveRoot) throws IOException {
        // create directory if it doesn't exist (needed for walk())
        Files.createDirectories(dir);
        try (var walk = Files.walk(dir)) {
            walk.skip(leaveRoot? 1 : 0)
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
    private static void tryFinally(Closeable tryBlock, Closeable finallyBlock)
            throws IOException {
        try (Closeable c = finallyBlock) {
            tryBlock.close();
        }
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
     * {@link Logger#errorReportSource}
     */
    private CharSequence formatSource(Diagnostic<?> problem) {
        char[] unitSource = null;
        try (var in = ((JavaFileObject) problem.getSource()).openInputStream()) {
            unitSource = new String(in.readAllBytes(), UTF_8).toCharArray();
        } catch (IOException e) {}

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
        for (int i = startPos; i <= (endPos >= len ? len - 1 : endPos); i++) {
            result.append('^');
        }
        return result;
    }
}
