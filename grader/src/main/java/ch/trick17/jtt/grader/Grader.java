package ch.trick17.jtt.grader;

import ch.trick17.jtt.memcompile.*;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testrunner.TestRunner;

import java.io.Closeable;
import java.io.IOException;
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
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.*;
import static java.util.List.copyOf;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

public class Grader implements Closeable {

    private static final Path DEFAULT_SOURCE_DIR = Path.of("src/test/java").toAbsolutePath();
    private static final Compiler DEFAULT_COMPILER = ECLIPSE;
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

    public Result grade(Task task, List<InMemSource> sources)
            throws IOException {
        for (var source : task.givenSources()) {
            sources.removeIf(s -> s.getPath().equals(source.getPath()));
            sources.add(source);
        }

        // compile submission
        InMemCompilation.Result compileResult;
        if (sources.isEmpty()) {
            compileResult = new InMemCompilation.Result(emptyList(), emptyList());
        } else {
            compileResult = InMemCompilation.compile(task.compiler(), sources,
                    ClassPath.fromFiles(task.dependencies()));
        }

        // compile tests
        var fileClassPath = stream(getProperty("java.class.path").split(pathSeparator))
                .map(Path::of)
                .collect(toCollection(ArrayList::new));
        fileClassPath.addAll(task.dependencies());
        var testCompileResult = InMemCompilation.compile(task.compiler(),
                task.testSources(), new ClassPath(compileResult.output(), fileClassPath));

        // run tests
        var compiled = !testCompileResult.output().isEmpty();
        List<TestResult> testResults = null;
        if (compiled) {
            testResults = runTests(task, compileResult.output(),
                    testCompileResult.output());
        }
        return new Result(compileResult.errors(), testCompileResult.errors(),
                compiled, testResults);
    }

    private List<TestResult> runTests(Task task,
                                 List<InMemClassFile> classes,
                                 List<InMemClassFile> testClasses) throws IOException {
        ClassPath sandboxedCode;
        ClassPath supportCode;
        if (task.restrictTests) {
            sandboxedCode = ClassPath.fromMemory(classes).withMemory(testClasses);
            supportCode = ClassPath.fromFiles(task.dependencies()).withCurrent();
        } else {
            sandboxedCode = ClassPath.fromMemory(classes);
            supportCode = ClassPath.fromMemory(testClasses)
                    .withFiles(task.dependencies()).withCurrent();
        }
        var testRunnerTask = new TestRunner.Task(task.testClassNames(),
                sandboxedCode, supportCode,
                task.repetitions(), task.repTimeout(), task.testTimeout(),
                task.permittedCalls(), task.testVmArgs());

        return testRunner.run(testRunnerTask).testResults();
    }

    @Override
    public void close() {
        testRunner.close();
    }

    public static class Task {

        private final List<InMemSource> testSources;
        private final List<InMemSource> givenSources;

        private Compiler compiler = DEFAULT_COMPILER;
        private int repetitions = DEFAULT_REPETITIONS;
        private Duration repTimeout = DEFAULT_REP_TIMEOUT;
        private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
        private String permittedCalls = Whitelist.DEFAULT_WHITELIST_DEF;
        private boolean restrictTests = false;
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
         * Sets to compiler to use. The default is {@link Compiler#ECLIPSE}
         */
        public Task compiler(Compiler compiler) {
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
         * Defines whether the whitelist of permitted method/constructor calls applies also
         * to the tests. The default is <code>false</code>.
         */
        public Task restrictTests(boolean restrictTests) {
            this.restrictTests = restrictTests;
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

        public boolean restrictTests() {
            return restrictTests;
        }

        public List<Path> dependencies() {
            return dependencies;
        }

        public List<String> testVmArgs() {
            return testVmArgs;
        }
    }

    public record Result(
            List<String> compileErrors,
            List<String> testCompileErrors,
            boolean compiled,
            List<TestResult> testResults) {

        public List<Property> properties() {
            var withNull = Stream.of(
                    !compileErrors.isEmpty() ? COMPILE_ERRORS : null,
                    !testCompileErrors.isEmpty() ? TEST_COMPILE_ERRORS : null,
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

        public Result with(List<TestResult> testResults) {
            return new Result(compileErrors, testCompileErrors, compiled, testResults);
        }
    }
}
