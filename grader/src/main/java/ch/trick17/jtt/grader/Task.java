package ch.trick17.jtt.grader;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

public class Task {

    private static final Path DEFAULT_TEST_SRC_DIR = Path.of("tests").toAbsolutePath();
    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);

    private final String testClass;
    private final Compiler compiler;

    private Path testSrcDir = DEFAULT_TEST_SRC_DIR;
    private final Set<String> filesToCopy;
    private int repetitions = DEFAULT_REPETITIONS;
    private Duration repTimeout = DEFAULT_REP_TIMEOUT;
    private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
    private boolean permRestrictions = true;

    public static Task fromClassName(String testClass) {
        return new Task(testClass, ECLIPSE);
    }

    public static Task fromClassName(String testClass, Compiler compiler) {
        return new Task(testClass, compiler);
    }

    private Task(String testClass, Compiler compiler) {
        this.testClass = requireNonNull(testClass);
        this.compiler = requireNonNull(compiler);
        filesToCopy = new HashSet<>(Set.of(testClass.replace('.', '/') + ".java"));
    }

    public Task testSrcDir(Path testSrcDir) {
        this.testSrcDir = testSrcDir.toAbsolutePath();
        return this;
    }

    /**
     * In addition to test class, copy these files. Relative to "test src" directory.
     */
    public Task copy(String... files) {
        filesToCopy.addAll(asList(files));
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

    public Task permRestrictions(boolean permRestrictions) {
        this.permRestrictions = permRestrictions;
        return this;
    }

    public String testClass() {
        return testClass;
    }

    public String testClassSimpleName() {
        var parts = testClass.split("\\.");
        return parts[parts.length - 1];
    }

    public Path testSrcDir() {
        return testSrcDir;
    }

    public Set<String> filesToCopy() {
        return unmodifiableSet(filesToCopy);
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

    public boolean permRestrictions() {
        return permRestrictions;
    }

    public Path resultFile() {
        return Path.of("results-" + testClassSimpleName() + ".tsv").toAbsolutePath();
    }
}
