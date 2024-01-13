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

    private static final Path DEFAULT_TEST_SRC_DIR = Path.of("src/test/java").toAbsolutePath();
    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);

    private final List<InMemSource> testClasses;
    private final List<InMemSource> givenClasses;

    private Compiler compiler = ECLIPSE;
    private int repetitions = DEFAULT_REPETITIONS;
    private Duration repTimeout = DEFAULT_REP_TIMEOUT;
    private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
    private String permittedCalls = Whitelist.DEFAULT_WHITELIST_DEF;
    private List<Path> dependencies = emptyList();

    public static Task fromString(String testClassCode) {
        return new Task(List.of(new InMemSource(testClassCode)), emptyList());
    }

    public static Task from(Class<?> testClass,
                            String... givenSrcFiles) throws IOException {
        return from(testClass, DEFAULT_TEST_SRC_DIR, givenSrcFiles);
    }

    public static Task from(Class<?> testClass, Path testSrcDir,
                            String... givenSrcFiles) throws IOException {
        return fromClassName(testClass.getName(), testSrcDir, givenSrcFiles);
    }

    public static Task fromClassName(String testClassName,
                                     String... givenSrcFiles) throws IOException {
        return fromClassName(testClassName, DEFAULT_TEST_SRC_DIR, givenSrcFiles);
    }

    public static Task fromClassName(String testClassName, Path testSrcDir,
                                     String... givenSrcFiles) throws IOException {
        return fromClassName(testClassName, testSrcDir, asList(givenSrcFiles), emptyList());
    }

    public static Task fromClassName(String testClassName, Path testSrcDir,
                                     List<String> givenSrcFiles,
                                     List<String> moreTestSrcFiles) throws IOException {
        var givenClasses = readSrcFiles(testSrcDir, givenSrcFiles);
        var testFile = testSrcDir.resolve(toPath(testClassName));
        var testClasses = new ArrayList<>(List.of(InMemSource.fromFile(testFile)));
        testClasses.addAll(readSrcFiles(testSrcDir, moreTestSrcFiles));
        return new Task(testClasses, givenClasses);
    }

    private static List<InMemSource> readSrcFiles(Path dir, List<String> srcFiles) throws IOException {
        var givenClasses = new ArrayList<InMemSource>();
        for (var file : srcFiles) {
            if (!file.endsWith(".java")) {
                throw new IllegalArgumentException("source file must end with .java");
            }
            givenClasses.add(InMemSource.fromFile(dir.resolve(file)));
        }
        return givenClasses;
    }

    private static Path toPath(String className) {
        return Path.of(className.replace('.', separatorChar) + ".java");
    }

    private Task(List<InMemSource> testClasses, List<InMemSource> givenClasses) {
        this.testClasses = testClasses;
        this.givenClasses = givenClasses;
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

    public String testClassName() {
        return testClasses.get(0).getFirstClassName();
    }

    public String testClassSimpleName() {
        var parts = testClassName().split("\\.");
        return parts[parts.length - 1];
    }

    public List<InMemSource> testClasses() {
        return unmodifiableList(testClasses);
    }

    public List<InMemSource> givenClasses() {
        return unmodifiableList(givenClasses);
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
}
