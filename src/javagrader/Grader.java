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
import java.util.regex.Pattern;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;

public class Grader {

    static {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2");
    }

    private static final Pattern lines = Pattern.compile("\r?\n");

    private final List<Task> tasks;
    private final Path root;
    private final Map<Task, Results> results;
    private Predicate<Path> filter = p -> true;
    private Path inspector;

    public Grader(List<Task> tasks, Path root) {
        this.tasks = requireNonNull(tasks);
        this.root = requireNonNull(root);
        results = tasks.stream().collect(toMap(t -> t, t -> new Results()));
    }

    public void gradeOnly(String... submissions) {
        var set = new HashSet<>(asList(submissions));
        filter = p -> set.contains(p.getFileName().toString());
    }

    public void run() throws IOException {
        var submissions = Files.list(root)
                .filter(Files::isDirectory)
                .filter(filter)
                .sorted()
                .collect(toList());

        inspector = copyInspector();
        try {
            var startTime = currentTimeMillis();
            var i = new AtomicInteger(0);
            submissions.stream().parallel().forEach(submission -> {
                var baos = new ByteArrayOutputStream();
                var out = new PrintStream(baos);
    
                out.println("Grading " + submission.getFileName());
                grade(submission, out);
                if (Math.random() > 0.75) {
                    writeResultsToFile();
                }
    
                out.println("Graded " + i.incrementAndGet() + "/" + submissions.size() +
                        " Total Time: " + ((currentTimeMillis() - startTime) / 1000) + " s");
                out.println();
                System.out.println(baos);
            });
        } finally {
            Files.delete(inspector);
        }

        writeResultsToFile();
        System.out.println(submissions.size() + " solutions processed");
    }

    private Path copyInspector() throws IOException {
        var path = Files.createTempFile("inspector", ".jar");
        try (var in = Grader.class.getResourceAsStream("inspector.jar");
                var out = Files.newOutputStream(path)) {
            in.transferTo(out);
        }
        return path;
    }

    private synchronized void writeResultsToFile() {
        for (var entry : results.entrySet()) {
            try {
                entry.getValue().writeTo(Path.of(entry.getKey().resultFileName()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void grade(Path submission, PrintStream out) {
        for (var task : tasks) {
            gradeTask(submission, task, out);
        }
    }

    private void gradeTask(Path submission, Task task, PrintStream out) {
        var projectPath = submission;
        if (task.directory().isPresent()) {
            projectPath = projectPath.resolve(task.directory().get());
        }
        var student = submission.getFileName().toString();

        results.get(task).addStudent(student);
        var compiled = compileProject(projectPath, task, out);
        if (compiled) {
            results.get(task).addCriterion(student, "compiles");
            runTests(task, projectPath, student, out);
        }
    }

    private boolean compileProject(Path projectPath, Task task,
            PrintStream out) {
        var srcPath = projectPath.resolve("src").toAbsolutePath();
        Set<Path> sources;
        try {
            // remove any pre-compiled class files from bin/
            var binPath = projectPath.resolve("bin");
            Files.createDirectories(binPath);
            Files.walk(binPath)
                    .skip(1) // skip bin/ folder itself
                    .map(Path::toFile)
                    .sorted(reverseOrder())
                    .forEach(File::delete);

            // Copy any properties files into bin folder
            // Create src directory in case it doesn't exist (yes, it happened)
            Files.createDirectories(srcPath);
            Files.walk(srcPath)
                    .filter(f -> f.toString().endsWith(".properties"))
                    .forEach(f -> {
                        try {
                            Path sourcePath = srcPath.resolve(f);
                            Path destPath = binPath.resolve(f.getFileName());
                            Files.copy(sourcePath, destPath);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

            // Copy test and additional files class into student's src/
            for (var file : task.filesToCopy()) {
                var from = Path.of("tests", file).toAbsolutePath();
                var to = srcPath.resolve(file);
                // classes (like HardTimeout) could be inside packages...
                Files.createDirectories(to.getParent());
                Files.copy(from, to, REPLACE_EXISTING);
            }

            sources = Files.walk(srcPath)
                    .filter(f -> f.toString().endsWith(".java"))
                    .collect(toCollection(HashSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var javac = getSystemJavaCompiler();

        while (true) {
            var collector = new DiagnosticCollector<>();
            var manager = javac.getStandardFileManager(collector, null, UTF_8);

            var options = asList(
                    "-cp", System.getProperty("java.class.path"),
                    "-d", projectPath.resolve("bin").toString());
            javac.getTask(null, manager, collector, options, null,
                    manager.getJavaFileObjectsFromPaths(sources)).call();

            var errors = collector.getDiagnostics().stream()
                    .filter(d -> d.getKind() == ERROR).collect(toList());
            if (errors.isEmpty()) {
                return true;
            }

            var faultyFiles = errors.stream()
                    .map(d -> (JavaFileObject) d.getSource())
                    .map(f -> srcPath.relativize(Path.of(f.getName())).toString())
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
                out.println("WARNING: One of " + task.filesToCopy() + " had a compile error.\n");
            }

            errors.forEach(out::println);

            if (faultyFiles.isEmpty()) {
                // no files left to remove. unable to compile.
                return false;
            } else {
                var faulty = faultyFiles.stream().findFirst().get();
                out.printf("%s appears to be faulty. Ignoring it for compilation.\n", faulty);
                sources.remove(srcPath.resolve(faulty));
            }
        }
    }

    private void runTests(Task task, Path projectPath, String student,
            PrintStream out) {
        try {
            var classes = Files.list(projectPath.resolve("src"))
                    .map(p -> p.getFileName().toString())
                    .filter(s -> s.endsWith(".java"))
                    .map(s -> s.substring(0, s.length() - 5)).collect(toList());
            var agentArg = "-javaagent:" + inspector.toAbsolutePath() + "="
                    + classes.stream().collect(joining(","));

            var junitArgs = new ArrayList<>(classes);
            junitArgs.add(0, task.testClass);
            var jUnit = new JavaProcessBuilder(TestRunner.class, junitArgs)
                    .classpath(projectPath.resolve("bin") + pathSeparator
                            + System.getProperty("java.class.path"))
                    .vmArgs("-Dfile.encoding=UTF8", agentArg, "-XX:-OmitStackTraceInFastThrow")
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
            lines.splitAsStream(jUnitOutput.toString())
                    .filter(line -> !line.isEmpty())
                    .forEach(line -> results.get(task).addCriterion(student, line));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
