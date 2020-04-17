package javagrader;

import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;

public class Grader {

    private static final Path GRADING_SRC = Path.of("src");
    private static final Path GRADING_BIN = Path.of("bin");

    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2");
    }

    private final List<Task> tasks;
    private final Path root;
    private final ProjectStructure structure;

    private final Map<Task, Results> results;
    private Predicate<Path> filter = p -> true;
    private Path inspector;

    public Grader(List<Task> tasks, Path root, ProjectStructure structure) {
        this.tasks = requireNonNull(tasks);
        this.root = requireNonNull(root);
        this.structure = requireNonNull(structure);
        results = tasks.stream().collect(toMap(t -> t, t -> new Results()));
    }

    public void gradeOnly(String... submNames) {
        var set = new HashSet<>(asList(submNames));
        filter = p -> set.contains(p.getFileName().toString());
    }

    public void run() throws IOException {
        List<Path> submissions;
        try (var list = Files.list(root)) {
            submissions = list
                    .filter(Files::isDirectory)
                    .filter(filter)
                    .sorted()
                    .collect(toList());
        }

        withInspector(() -> {
            var startTime = currentTimeMillis();
            var i = new AtomicInteger(0);
            submissions.stream().parallel().forEach(subm -> {
                var bytes = new ByteArrayOutputStream();
                var out = new PrintStream(bytes);
    
                out.println("Grading " + subm.getFileName());
                try {
                    grade(subm, out);
                    if (Math.random() > 0.75) {
                        writeResultsToFile();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                out.printf("Graded %d/%d, total time: %d s\n\n",
                        i.incrementAndGet(), submissions.size(),
                        (currentTimeMillis() - startTime) / 1000);
                System.out.println(bytes);
            });
        });

        writeResultsToFile();
        System.out.println(submissions.size() + " submissions processed");
    }

    private void withInspector(Runnable code) throws IOException {
        inspector = Files.createTempFile("inspector", ".jar");
        // make sure exceptions from "code" are not suppressed:
        try (Closeable deleter = () -> Files.delete(inspector)) {
            try (var in = Grader.class.getResourceAsStream("inspector.jar")) {
                Files.copy(in, inspector, REPLACE_EXISTING);
            }
            code.run();
        }
    }

    private synchronized void writeResultsToFile() throws IOException {
        for (var entry : results.entrySet()) {
            entry.getValue().writeTo(Path.of(entry.getKey().resultFileName()));
        }
    }

    private void grade(Path subm, PrintStream out) throws IOException {
        for (var task : tasks) {
            gradeTask(subm, task, out);
        }
    }

    private void gradeTask(Path subm, Task task, PrintStream out)
            throws IOException {
        var projDir = subm;
        if (task.directory().isPresent()) {
            projDir = projDir.resolve(task.directory().get());
        }
        var gradingDir = projDir.resolve(task.gradingDir());

        try {
            var submName = subm.getFileName().toString();
            results.get(task).addSubmission(submName);

            prepareProject(projDir, task);
            var compiled = compileProject(task, gradingDir, out);
            if (compiled) {
                results.get(task).addCriterion(submName, "compiles");
                runTests(task, gradingDir, submName, out);
            }
        } finally {
            delete(gradingDir, false);
        }
    }

    private void prepareProject(Path projDir, Task task) throws IOException {
        // Remove any grading files from previous runs
        var gradingDir = projDir.resolve(task.gradingDir());
        delete(gradingDir, true);

        // Copy sources
        var origSrcDir = projDir.resolve(structure.src);
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
            var from = Path.of("tests", file);
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

    private boolean compileProject(Task task, Path gradingDir,
            PrintStream out) throws IOException {
        var srcDir = gradingDir.resolve(GRADING_SRC);

        Set<Path> sources;
        try (var walk = Files.walk(srcDir)) {
            sources = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(toCollection(HashSet::new));
        }

        var javac = getSystemJavaCompiler();

        while (true) {
            var collector = new DiagnosticCollector<>();
            var manager = javac.getStandardFileManager(collector, null, UTF_8);

            var options = asList(
                    "-cp", System.getProperty("java.class.path"),
                    "-d", gradingDir.resolve(GRADING_BIN).toString());
            javac.getTask(null, manager, collector, options, null,
                    manager.getJavaFileObjectsFromPaths(sources)).call();

            var errors = collector.getDiagnostics().stream()
                    .filter(d -> d.getKind() == ERROR).collect(toList());
            if (errors.isEmpty()) {
                return true;
            }

            var faultyFiles = errors.stream()
                    .map(d -> (JavaFileObject) d.getSource())
                    .map(f -> srcDir.relativize(Path.of(f.getName())).toString())
                    .map(s -> s.replace(separatorChar, '/'))
                    .collect(toSet());
            if (task.classUnderTest.isPresent()) {
                var cutPath = task.classUnderTest.get().replace('.', '/') + ".java";
                if (faultyFiles.remove(cutPath)) {
                    // never remove class under test from compile arguments
                    out.println("Class under test has errors.");
                }
            }
            if (faultyFiles.removeAll(task.filesToCopy())) {
                // copy-in files should *never* have errors
                out.println("WARNING: One of " + task.filesToCopy()
                        + " had a compile error.\n");
            }

            errors.forEach(out::println);

            if (faultyFiles.isEmpty()) {
                // no files left to remove. unable to compile.
                return false;
            } else {
                var faulty = faultyFiles.iterator().next();
                out.printf("%s seems to be faulty. Ignoring it for compilation.\n",
                        faulty);
                sources.remove(srcDir.resolve(faulty));
            }
        }
    }

    private void runTests(Task task, Path gradingDir, String submName,
            PrintStream out) throws IOException {
        var agentArg = "-javaagent:" + inspector + "="
                + classesToInspect(gradingDir.resolve(GRADING_SRC));

        var jUnit = new JavaProcessBuilder(TestRunner.class, task.testClass)
                .classpath(gradingDir.resolve(GRADING_BIN) + pathSeparator
                        + System.getProperty("java.class.path"))
                .vmArgs("-Dfile.encoding=UTF8", agentArg,
                        "-XX:-OmitStackTraceInFastThrow")
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
                .filter(line -> !line.isEmpty())
                .forEach(line -> results.get(task).addCriterion(submName, line));

    }

    private String classesToInspect(Path srcDir) throws IOException {
        try (var all = Files.walk(srcDir)) {
            return all
                    .map(p -> srcDir.relativize(p).toString())
                    .filter(s -> s.endsWith(".java"))
                    .map(s -> s.substring(0, s.length() - 5))
                    .map(s -> s.replace(separatorChar, '.'))
                    .collect(joining(","));
        }
    }

    private void delete(Path dir, boolean leaveRoot) throws IOException {
        // create directory if it doesn't exist (needed for walk())
        Files.createDirectories(dir);
        try (var walk = Files.walk(dir)) {
            walk.skip(leaveRoot? 1 : 0)
                .map(Path::toFile)
                .sorted(reverseOrder())
                .forEach(File::delete);
        }
    }
}
