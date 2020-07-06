package ch.trick17.jtt.grader;

import static java.io.File.separator;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class Task {

    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);

    public final String testClass;

    private final Set<String> filesToCopy;
    private Optional<Path> dir = Optional.empty();
    private int repetitions = DEFAULT_REPETITIONS;
    private Duration repTimeout = DEFAULT_REP_TIMEOUT;
    private Duration testTimeout = DEFAULT_TEST_TIMEOUT;



    public Task(Class<?> testClass) {
        this(testClass.getName());
    }

    public Task(String testClass) {
        this(testClass, true);
    }

    private Task(String testClass, boolean internal) {
        this.testClass = requireNonNull(testClass);

        filesToCopy = new HashSet<>(Set.of(testClass.replace('.', '/') + ".java"));
    }

    /**
     * Submissions are located in given subdirectory.
     */
    public Task in(String dir) {
        this.dir = Optional.of(Path.of(dir));
        return this;
    }

    /**
     * In addition to test class, copy these files. Relative to "tests" directory.
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

    public String testClassSimpleName() {
        var parts = testClass.split("\\.");
        return parts[parts.length - 1];
    }

    public Set<String> filesToCopy() {
        return unmodifiableSet(filesToCopy);
    }

    public Optional<Path> directory() {
        return dir;
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

    public Path resultFile() {
        var name = testClassSimpleName();
        if (dir.isPresent()) {
            name = dir.get().toString().replace(separator, "-") + "-" + name;
        }
        return Path.of("results-" + name + ".tsv").toAbsolutePath();
    }

    public Path gradingDir() {
        return Path.of("grading-" + testClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClass, dir);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Task)) {
            return false;
        }
        var other = (Task) obj;
        return testClass.equals(other.testClass)
                && dir.equals(other.dir);
    }
}
