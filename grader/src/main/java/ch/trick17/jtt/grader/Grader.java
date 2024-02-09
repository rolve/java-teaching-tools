package ch.trick17.jtt.grader;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.memcompile.InMemCompilation;
import ch.trick17.jtt.memcompile.InMemCompilation.Result;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testrunner.TestRunConfig;
import ch.trick17.jtt.testrunner.TestRunner;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.pathSeparator;
import static java.lang.String.*;
import static java.lang.System.getProperty;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

public class Grader implements Closeable {

    private final TestRunner testRunner = new TestRunner();

    public GradeResult grade(Task task, Submission subm) throws IOException {
        return grade(task, subm, System.out);
    }

    public GradeResult grade(Task task, Submission subm, PrintStream out)
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
        return new GradeResult(subm.name(), compileResult.errors(),
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

        var failMsgs = results.stream()
                .flatMap(r -> r.exceptions().stream())
                .map(e -> format("%s: %s", e.getClass().getName(),
                        valueOf(e.getMessage()).replaceAll("\\s+", " ")))
                .filter(msg -> !msg.startsWith("java.lang.Error: Unresolved compilation problems:"))
                .distinct()
                .toList();
        if (!failMsgs.isEmpty()) {
            out.println("Tests failed for the following reasons:");
            failMsgs.forEach(msg -> out.print(msg.indent(2))); // indent includes \n
        }

        for (var res : results) {
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

    @Override
    public void close() {
        testRunner.close();
    }
}
