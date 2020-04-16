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
        var submissions = Files.list(root)
                .filter(Files::isDirectory)
                .filter(filter)
                .sorted()
                .collect(toList());

        inspector = copyInspector();
        try {
            var startTime = currentTimeMillis();
            var i = new AtomicInteger(0);
            submissions.stream().parallel().forEach(subm -> {
                var baos = new ByteArrayOutputStream();
                var out = new PrintStream(baos);
    
                out.println("Grading " + subm.getFileName());
                grade(subm, out);
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
        System.out.println(submissions.size() + " submissions processed");
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

    private void grade(Path subm, PrintStream out) {
        for (var task : tasks) {
            gradeTask(subm, task, out);
        }
    }

    private void gradeTask(Path subm, Task task, PrintStream out) {
        var projDir = subm;
        if (task.directory().isPresent()) {
            projDir = projDir.resolve(task.directory().get());
        }
        var submName = subm.getFileName().toString();

        results.get(task).addSubmission(submName);
        var compiled = compileProject(projDir, task, out);
        if (compiled) {
            results.get(task).addCriterion(submName, "compiles");
            runTests(task, projDir, submName, out);
        }
    }

    private boolean compileProject(Path projDir, Task task, PrintStream out) {
        var srcDir = projDir.resolve(structure.src)
                .toAbsolutePath();
        Set<Path> sources;
        try {
            // remove any pre-compiled class files from bin dir
            var binDir = projDir.resolve(structure.bin);
            Files.createDirectories(binDir);
            Files.walk(binDir)
                    .skip(1) // skip bin directory itself
                    .map(Path::toFile)
                    .sorted(reverseOrder())
                    .forEach(File::delete);

            // Copy any properties files into bin directory
            // Create src directory in case it doesn't exist (yes, it happened)
            Files.createDirectories(srcDir);
            Files.walk(srcDir)
                    .filter(f -> f.toString().endsWith(".properties"))
                    .forEach(f -> {
                        try {
                            var srcPath = srcDir.resolve(f);
                            var dstPath = binDir.resolve(f);
                            Files.copy(srcPath, dstPath);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

            // Copy test and additional files class into student's src
            for (var file : task.filesToCopy()) {
                var from = Path.of("tests", file).toAbsolutePath();
                var to = srcDir.resolve(file);
                // classes could be inside packages...
                Files.createDirectories(to.getParent());
                Files.copy(from, to, REPLACE_EXISTING);
            }

            sources = Files.walk(srcDir)
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
                    "-d", projDir.resolve(structure.bin).toString());
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

    private void runTests(Task task, Path projDir, String submName,
            PrintStream out) {
        try {
            var agentArg = "-javaagent:" + inspector + "="
                    + classesToInspect(projDir.resolve(structure.src));

            var jUnit = new JavaProcessBuilder(TestRunner.class, task.testClass)
                    .classpath(projDir.resolve(structure.bin) + pathSeparator
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
            lines.splitAsStream(jUnitOutput.toString())
                    .filter(line -> !line.isEmpty())
                    .forEach(line -> results.get(task).addCriterion(submName, line));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
}
