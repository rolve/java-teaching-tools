package ch.trick17.jtt.grader;

import ch.trick17.jtt.memcompile.Compiler;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.sandbox.Whitelist;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static java.io.File.separatorChar;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.List.copyOf;

public class Task {

    private static final Path DEFAULT_SOURCE_DIR = Path.of("src/test/java").toAbsolutePath();
    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final List<String> DEFAULT_TEST_VM_ARGS = List.of("-Dfile.encoding=UTF8");

    private final List<InMemSource> testSources;
    private final List<InMemSource> givenSources;

    private Compiler compiler = ECLIPSE;
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

    private Task(List<InMemSource> testSources, List<InMemSource> givenSources) {
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

    public String testClassName() {
        return testSources.get(0).getPath()
                .replace('/', '.').replace(".java", "");
    }

    public String testClassSimpleName() {
        var parts = testClassName().split("\\.");
        return parts[parts.length - 1];
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
