package javagrader;

import static java.io.File.separator;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.*;

public class Task {

    public final String testClass;
    public final Optional<String> classUnderTest;

    private final Set<String> filesToCopy;
    private Optional<Path> dir = Optional.empty();

    public Task(Class<?> testClass) {
        this(testClass.getName());
    }

    public Task(String testClass) {
        this(testClass, null, true);
    }

    public Task(Class<?> testClass, Class<?> classUnderTest) {
        this(testClass.getName(), classUnderTest.getName());
    }

    public Task(String testClass, String classUnderTest) {
        this(testClass, requireNonNull(classUnderTest), true);
    }

    private Task(String testClass, String classUnderTest, boolean internal) {
        this.testClass = requireNonNull(testClass);
        this.classUnderTest = Optional.ofNullable(classUnderTest); // may be null if not needed

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

    public Set<String> filesToCopy() {
        return unmodifiableSet(filesToCopy);
    }

    public Optional<Path> directory() {
        return dir;
    }

    public Path resultFile() {
        var parts = testClass.split("\\.");
        var className = parts[parts.length - 1];
        var name = className + "-results.tsv";
        if (dir.isPresent()) {
            name = dir.get().toString().replace(separator, "-") + "-" + name;
        }
        return Path.of(name);
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
