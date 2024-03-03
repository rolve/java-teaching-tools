package ch.trick17.jtt.grader;

import ch.trick17.jtt.memcompile.Compiler;
import ch.trick17.jtt.memcompile.*;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testrunner.TestRunner;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ch.trick17.jtt.grader.Property.*;
import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.lang.String.valueOf;
import static java.lang.String.*;
import static java.lang.System.getProperty;
import static java.nio.file.Files.list;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

public class Grader implements Closeable {

    private static final Path DEFAULT_SOURCE_DIR = Path.of("src/test/java").toAbsolutePath();
    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final List<String> DEFAULT_TEST_VM_ARGS = List.of("-Dfile.encoding=UTF8");

    private final TestRunner testRunner;

    public Grader() {
        this(new TestRunner());
    }

    public Grader(TestRunner testRunner) {
        this.testRunner = testRunner;
    }

    public Result grade(Task task, Submission subm) throws IOException {
        return grade(task, subm, System.out);
    }

    public Result grade(Task task, Submission subm, PrintStream out)
            throws IOException {
        var sources = collectSources(subm.srcDir());
        for (var source : task.givenSources()) {
            sources.removeIf(s -> s.getPath().equals(source.getPath()));
            sources.add(source);
        }

        // compile submission
        InMemCompilation.Result compileResult;
        if (sources.isEmpty()) {
            compileResult = new InMemCompilation.Result(false, emptyList());
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
        return new Result(compileResult.errors(), testCompileResult.errors(),
                compiled, testResults);
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
        var testRunnerTask = new TestRunner.Task(task.testClassNames(),
                ClassPath.fromMemory(classes), supportCode,
                task.repetitions(), task.repTimeout(), task.testTimeout(),
                task.permittedCalls(), task.testVmArgs());

        var results = testRunner.run(testRunnerTask).testResults();

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

    public static class Task {

        private final List<InMemSource> testSources;
        private final List<InMemSource> givenSources;

        private ch.trick17.jtt.memcompile.Compiler compiler = ECLIPSE;
        private int repetitions = DEFAULT_REPETITIONS;
        private Duration repTimeout = DEFAULT_REP_TIMEOUT;
        private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
        private String permittedCalls = Whitelist.DEFAULT_WHITELIST_DEF;
        private List<Path> dependencies = emptyList();
        private List<String> testVmArgs = DEFAULT_TEST_VM_ARGS;

        public static Task fromString(String testClassCode) {
            return new Task(List.of(InMemSource.fromString(testClassCode)), emptyList());
        }

        public static Task from(Class<?> testClass,
                                String... givenSourceFiles) throws IOException {
            return from(testClass, DEFAULT_SOURCE_DIR, givenSourceFiles);
        }

        public static Task from(Class<?> testClass, Path sourceDir,
                                String... givenSourceFiles) throws IOException {
            return fromClassName(testClass.getName(), sourceDir, givenSourceFiles);
        }

        public static Task fromClassName(String testClassName,
                                         String... givenSourceFiles) throws IOException {
            return fromClassName(testClassName, DEFAULT_SOURCE_DIR, givenSourceFiles);
        }

        public static Task fromClassName(String testClassName, Path sourceDir,
                                         String... givenSourceFiles) throws IOException {
            return fromClassName(testClassName, sourceDir, asList(givenSourceFiles), emptyList());
        }

        public static Task fromClassName(String testClassName, Path sourceDir,
                                         List<String> givenSourceFiles,
                                         List<String> moreTestSourceFiles) throws IOException {
            var givenSources = readSourceFiles(sourceDir, givenSourceFiles);
            var testSourceFile = sourceDir.resolve(toPath(testClassName));
            var testSources = new ArrayList<InMemSource>();
            testSources.add(InMemSource.fromFile(testSourceFile, sourceDir));
            testSources.addAll(readSourceFiles(sourceDir, moreTestSourceFiles));
            return new Task(testSources, givenSources);
        }

        private static List<InMemSource> readSourceFiles(Path sourceDir,
                                                         List<String> sourceFiles) throws IOException {
            var givenClasses = new ArrayList<InMemSource>();
            for (var file : sourceFiles) {
                if (!file.endsWith(".java")) {
                    throw new IllegalArgumentException("source file must end with .java");
                }
                givenClasses.add(InMemSource.fromFile(sourceDir.resolve(file), sourceDir));
            }
            return givenClasses;
        }

        private static Path toPath(String className) {
            return Path.of(className.replace('.', separatorChar) + ".java");
        }

        public Task(List<InMemSource> testSources, List<InMemSource> givenSources) {
            this.testSources = testSources;
            this.givenSources = givenSources;
        }

        /**
         * Sets to compiler to use. The default is {@link ch.trick17.jtt.memcompile.Compiler#ECLIPSE}
         */
        public Task compiler(ch.trick17.jtt.memcompile.Compiler compiler) {
            this.compiler = compiler;
            return this;
        }

        /**
         * Defines the number of times a single test is executed ("repeated"). Must
         * be at least 1.
         */
        public Task repetitions(int repetitions) {
            if (repetitions < 1) {
                throw new IllegalArgumentException();
            }
            this.repetitions = repetitions;
            return this;
        }

        /**
         * Sets the timeouts to be used for test execution. The repetition timeout
         * is a hard timeout per execution ("repetition") of each test, while the
         * test timeout concerns all repetitions of a single test together; once the
         * test timeout has been reached, no more repetitions are attempted, but the
         * currently running repetition continues to execute, until it is finished
         * or the repetition timeout is reached.
         */
        public Task timeouts(Duration repTimeout, Duration testTimeout) {
            if (repTimeout.isNegative() || repTimeout.isZero()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            if (testTimeout.compareTo(repTimeout) < 0) {
                throw new IllegalArgumentException("test timeout must be >= repetition timeout");
            }
            this.repTimeout = repTimeout;
            this.testTimeout = testTimeout;
            return this;
        }

        /**
         * Sets the whitelist of permitted method/constructor calls, in the
         * following format:
         * <pre>
         *     package.Class1.*
         *
         *     package.Class2.method1
         *     package.Class2.method2(java.util.String)
         *     package.Class2.&lt;init&gt;
         *
         *     package.Class3.&lt;init&gt;(int)
         *     package.Class3.&lt;init&gt;(double)
         * </pre>
         * <p>
         * If set to <code>null</code>, all method/constructor calls are permitted.
         * <p>
         * By default, {@link Whitelist#DEFAULT_WHITELIST_DEF} is used.
         */
        public Task permittedCalls(String permittedCalls) {
            this.permittedCalls = permittedCalls;
            return this;
        }

        /**
         * Sets the given list of paths (to JAR files or directories) as the
         * dependencies that will be added to the class path for compilation and
         * test execution, in addition to the class path of the current JVM.
         */
        public Task dependencies(Path... dependencies) {
            return dependencies(asList(dependencies));
        }

        /**
         * Sets the given list of paths (to JAR files or directories) as the
         * dependencies that will be added to the class path for compilation and
         * test execution, in addition to the class path of the current JVM.
         */
        public Task dependencies(List<Path> dependencies) {
            this.dependencies = copyOf(dependencies);
            return this;
        }

        /**
         * Sets the VM arguments that are used to start the JVM(s) in which the
         * tests are executed (in addition to predefined arguments such as the
         * classpath, which is equal to the one of this VM). The default is
         * "-Dfile.encoding=UTF8", so to enforce a different (or again the same)
         * encoding, a respective argument should be included when using this
         * method.
         */
        public Task testVmArgs(String... testVmArgs) {
            return testVmArgs(asList(testVmArgs));
        }

        /**
         * Sets the VM arguments that are used to start the JVM(s) in which the
         * tests are executed (in addition to predefined arguments such as the
         * classpath, which is equal to the one of this VM). The default is
         * "-Dfile.encoding=UTF8", so to enforce a different (or again the same)
         * encoding, a respective argument should be included when using this
         * method.
         */
        public Task testVmArgs(List<String> testVmArgs) {
            this.testVmArgs = copyOf(testVmArgs);
            return this;
        }

        public List<String> testClassNames() {
            return testSources.stream()
                    .map(s -> s.getPath().replace('/', '.').replaceAll("\\.java$", ""))
                    .toList();
        }

        public List<InMemSource> testSources() {
            return unmodifiableList(testSources);
        }

        public List<InMemSource> givenSources() {
            return unmodifiableList(givenSources);
        }

        public Compiler compiler() {
            return compiler;
        }

        public int repetitions() {
            return repetitions;
        }

        public Duration repTimeout() {
            return repTimeout;
        }

        public Duration testTimeout() {
            return testTimeout;
        }

        public String permittedCalls() {
            return permittedCalls;
        }

        public List<Path> dependencies() {
            return dependencies;
        }

        public List<String> testVmArgs() {
            return testVmArgs;
        }
    }

    public record Submission(String name, Path srcDir) {

        public Submission {
            name = requireNonNull(name);
            srcDir = srcDir.toAbsolutePath().normalize();
        }

        /**
         * Loads multiple submissions from a <code>root</code> directory. Each
         * direct subdirectory in that directory is considered a submission. The
         * source directory of each submission is determined by resolving
         * <code>srcDir</code> (a relative path) against the submission directory.
         * For example, given the following directory structure:
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
         * "src/main/java" as <code>srcDir</code>, this method would return a list
         * containing three submissions, with names "bar", "baz", and "foo"
         * (submissions are sorted by name) and source directories
         * "/submissions/bar/src/main/java", "/submissions/baz/src/main/java",
         * and "/submissions/foo/src/main/java".
         */
        public static List<Submission> loadAllFrom(Path root, Path srcDir) throws IOException {
            if (srcDir.isAbsolute()) {
                throw new IllegalArgumentException("srcDir must be a relative path");
            }
            try (var list = list(root)) {
                return list
                        .filter(Files::isDirectory)
                        .map(dir -> new Submission(dir.getFileName().toString(), dir.resolve(srcDir)))
                        .sorted(comparing(Submission::name))
                        .toList();
            }
        }
    }

    public record Result(
            boolean compileErrors,
            boolean testCompileErrors,
            boolean compiled,
            List<TestResult> testResults) {

        public List<Property> properties() {
            var withNull = Stream.of(
                    compileErrors ? COMPILE_ERRORS : null,
                    testCompileErrors ? TEST_COMPILE_ERRORS : null,
                    compiled ? COMPILED : null,
                    anyMatch(TestResult::nonDeterm) ? NONDETERMINISTIC : null,
                    anyMatch(TestResult::timeout) ? TIMEOUT : null,
                    anyMatch(TestResult::outOfMemory) ? OUT_OF_MEMORY : null,
                    anyMatch(TestResult::incompleteReps) ? INCOMPLETE_REPETITIONS : null,
                    anyMatch(m -> !m.illegalOps().isEmpty()) ? ILLEGAL_OPERATION : null);
            return withNull
                    .filter(Objects::nonNull)
                    .toList();
        }

        private boolean anyMatch(Predicate<TestResult> predicate) {
            return compiled && testResults.stream().anyMatch(predicate);
        }

        /**
         * Tags are not supported right now, so this method returns an empty set.
         */
        public Set<String> tags() {
            return emptySet();
        }

        /**
         * Returns all tests that were executed for the submission, in the order of
         * execution. If no tests could be executed, the list is empty.
         */
        public List<TestMethod> allTests() {
            return Stream.ofNullable(testResults)
                    .flatMap(List::stream)
                    .map(TestResult::method)
                    .toList();

        }

        public List<TestMethod> passedTests() {
            return Stream.ofNullable(testResults)
                    .flatMap(List::stream)
                    .filter(TestResult::passed)
                    .map(TestResult::method)
                    .toList();
        }

        public List<TestMethod> failedTests() {
            return Stream.ofNullable(testResults)
                    .flatMap(List::stream)
                    .filter(not(TestResult::passed))
                    .map(TestResult::method)
                    .toList();
        }

        public TestResult testResultFor(String testName) {
            return testResults.stream()
                    .filter(r -> r.method().name().equals(testName))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
