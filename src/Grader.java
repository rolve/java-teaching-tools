import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
     * List of grading tasks. Modify this.
     */
    private static final List<Task> TASKS = List.of(
        new Task("u07", SpecialLinkedIntList.class, SplitTest.class, "HardTimeout.java", "SpecialIntNode.java")
    );

    /**
     * Run with the "root" directory (where all student's solutions are stored)
     * as the first argument.
     */
    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args[0]);
        new Grader(root).run();
    }

    private Path root;
    private Map<Task, Results> results;

    public Grader(Path root) {
        this.root = root;
        results = TASKS.stream().collect(toUnmodifiableMap(t -> t, t -> new Results()));
    }
    

    private void run() throws IOException {
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
                entry.getValue().writeTo(Paths.get(entry.getKey().resultFileName()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void grade(Path solution, PrintStream out) {
        for (Task task : TASKS) {
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
            Files.list(binPath).map(Path::toFile).filter(File::isFile)
                    .forEach(f -> f.delete());

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

            // Copy GradingTests class into student's src/
            for (String f : task.filesToCopy) {
                Path filePath = Paths.get("tests", f).toAbsolutePath();
                Files.copy(filePath, srcPath.resolve(f), REPLACE_EXISTING);
            }

            sources = new HashSet<>(Files.walk(srcPath)
                    .filter(f -> f.toString().endsWith(".java"))
                    .collect(toSet()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var classpath = Paths.get("lib", "junit.jar").toAbsolutePath() + pathSeparator +
                Paths.get("lib","hamcrest.jar").toAbsolutePath() + pathSeparator +
                Paths.get("lib","asm-7.0.jar").toAbsolutePath() + pathSeparator +
                Paths.get("inspector.jar").toAbsolutePath() + pathSeparator +
                "conf";
        

        var javac = getSystemJavaCompiler();

        while (true) {
            var collector = new DiagnosticCollector<>();
            var manager = javac.getStandardFileManager(collector, null, UTF_8);
            
            var options = asList(
                    "-cp", classpath,
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
                    .map(f -> srcPath.relativize(Paths.get(f.getName())).toString())
                    .collect(toSet());
            if (task.classUnderTest.isPresent()) {
                if (faultyFiles.remove(task.classUnderTest.get().getName().replace('.', separatorChar) + ".java")) {
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
            var agentArg = "-javaagent:inspector.jar=" + task.instrThreshold
                    + "," + classes.stream().collect(joining(","));

            var junitArgs = new ArrayList<>(classes);
            junitArgs.add(0, task.testClass.getName());
            var jUnitBuilder = new JavaProcessBuilder(TestRunner.class, junitArgs);
            jUnitBuilder
                    .classpath(projectPath.resolve("bin") + pathSeparator + jUnitBuilder.classpath())
                    .vmArgs("-Dfile.encoding=UTF8", agentArg, "-XX:-OmitStackTraceInFastThrow");

            var jUnit = jUnitBuilder.build().start();

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
