package ch.trick17.jtt.grader;

import ch.trick17.jtt.sandbox.Whitelist;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static java.io.File.separatorChar;
import static java.nio.file.Files.readString;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.List.copyOf;

public class Task {

    private static final Path DEFAULT_TEST_SRC_DIR = Path.of("src/test/java").toAbsolutePath();
    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);

    private static final Pattern PACKAGE_NAME = Pattern.compile("\\bpackage\\s+([^\\s;]+)");
    private static final Pattern CLASS_NAME = Pattern.compile("\\bclass\\s+([^\\s{]+)");

    private final String testClassName;
    private final Map<Path, String> testClasses;
    private final Map<Path, String> givenClasses;

    private Compiler compiler = ECLIPSE;
    private int repetitions = DEFAULT_REPETITIONS;
    private Duration repTimeout = DEFAULT_REP_TIMEOUT;
    private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
    private String permittedCalls = Whitelist.DEFAULT_WHITELIST_DEF;
    private List<Path> dependencies = emptyList();

    public static Task fromString(String testClassCode) {
        var simpleName = firstMatch(testClassCode, CLASS_NAME)
                .orElseThrow(() -> new IllegalArgumentException("no class name found"));
        var testClassName = firstMatch(testClassCode, PACKAGE_NAME)
                .map(packageName -> packageName + "." + simpleName)
                .orElse(simpleName);
        return new Task(testClassName, Map.of(toPath(testClassName), testClassCode), emptyMap());
    }

    private static Optional<String> firstMatch(String code, Pattern pattern) {
        return code.lines()
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .findFirst();
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
        var testClasses = readSrcFiles(testSrcDir, moreTestSrcFiles);
        var testPath = toPath(testClassName);
        testClasses.put(testPath, readString(testSrcDir.resolve(testPath)));
        return new Task(testClassName, testClasses, givenClasses);
    }

    private static Map<Path, String> readSrcFiles(Path dir, List<String> srcFiles) throws IOException {
        var givenClasses = new HashMap<Path, String>();
        for (var file : srcFiles) {
            if (!file.endsWith(".java")) {
                throw new IllegalArgumentException("source file must end with .java");
            }
            var path = Path.of(file);
            givenClasses.put(path, readString(dir.resolve(path)));
        }
        return givenClasses;
    }

    private static Path toPath(String className) {
        return Path.of(className.replace('.', separatorChar) + ".java");
    }

    private Task(String testClassName,
                 Map<Path, String> testClasses,
                 Map<Path, String> givenClasses) {
        this.testClassName = testClassName;
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
        return testClassName;
    }

    public String testClassSimpleName() {
        var parts = testClassName.split("\\.");
        return parts[parts.length - 1];
    }

    public Map<Path, String> testClasses() {
        return unmodifiableMap(testClasses);
    }

    public Map<Path, String> givenClasses() {
        return unmodifiableMap(givenClasses);
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
