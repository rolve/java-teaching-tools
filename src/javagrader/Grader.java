package javagrader;

import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;

public class Grader {

    private static final Pattern lines = Pattern.compile("\r?\n");

    /**
     * Run with the "root" directory (where all student's solutions are stored)
     * as the first argument.
     */
    public static void main(String[] args) throws IOException {
        var root = Path.of(args[0]);
        var tasks = List.of(
                new Task("u04", "OhneSieben", "OhneSiebenTest", Set.of()));
        new Grader(tasks, root).run();
    }

    private final List<Task> tasks;
    private final Path root;
    private final Map<Task, Results> results;

    public Grader(List<Task> tasks, Path root) {
        this.tasks = requireNonNull(tasks);
        this.root = requireNonNull(root);
        results = tasks.stream().collect(toMap(t -> t, t -> new Results()));
    }

    public void run() throws IOException {
        List<Path> solutions = Files.list(root)
                .filter(Files::isDirectory)
                //.filter(s -> Set.of("yforrer").contains(s.getFileName().toString()))
                .sorted()
                .collect(toList());

        long startTime = System.currentTimeMillis();
        AtomicInteger i = new AtomicInteger(0);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2");
        solutions.stream().parallel().forEach(solution -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(baos);

            out.println("Grading " + solution.getFileName());
            grade(solution, out);
            if (Math.random() > 0.75) {
                writeResultsToFile();
            }

            out.println("Graded " + i.incrementAndGet() + "/" + solutions.size() +
                    " Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " s");
            out.println();
            System.out.println(baos);
        });

        writeResultsToFile();
        System.out.println(solutions.size() + " solutions processed");
    }

    private synchronized void writeResultsToFile() {
        for (Entry<Task, Results> entry : results.entrySet()) {
            try {
                entry.getValue().writeTo(Path.of(entry.getKey().resultFileName()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void grade(Path solution, PrintStream out) {
        for (Task task : tasks) {
            gradeTask(solution, task, out);
        }
    }

    private void gradeTask(Path solution, Task task, PrintStream out) {
        Path projectPath = solution.resolve(task.projectName);
        String student = solution.getFileName().toString();

        results.get(task).addStudent(student);
        boolean compiled = compileProject(projectPath, task, out);
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
            for (var file : task.filesToCopy) {
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
            if (faultyFiles.removeAll(task.filesToCopy)) {
                // copy-in files should *never* have errors
                out.println("WARNING: One of " + task.filesToCopy + " had a compile error.\n");
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
            var agentArg = "-javaagent:inspector.jar="
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
