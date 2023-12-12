package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class TestRunConfig {

    // good candidate for a record, once we want to update to that Java version

    private final String testClassName;
    private final Path codeUnderTest;
    private final Path testCode;
    private final int repetitions;
    private final Duration repTimeout;
    private final Duration testTimeout;
    private final boolean permRestrictions;
    private final List<Path> dependencies = new ArrayList<>();

    public TestRunConfig(
            String testClassName,
            Path codeUnderTest,
            Path testCode,
            int repetitions,
            Duration repTimeout,
            Duration testTimeout,
            boolean permRestrictions,
            List<Path> dependencies) {
        this.testClassName = requireNonNull(testClassName);
        this.codeUnderTest = codeUnderTest;
        this.testCode = testCode;
        this.repetitions = repetitions;
        this.repTimeout = requireNonNull(repTimeout);
        this.testTimeout = requireNonNull(testTimeout);
        this.permRestrictions = permRestrictions;
        this.dependencies.addAll(dependencies);
    }

    @JsonCreator
    public TestRunConfig(
            String testClassName,
            String codeUnderTestString,
            String testCodeString,
            int repetitions,
            int repTimeoutMillis,
            int testTimeoutMillis,
            boolean permRestrictions,
            List<String> dependenciesStrings) {
        this(testClassName,
                Path.of(codeUnderTestString),
                Path.of(testCodeString),
                repetitions,
                Duration.ofMillis(repTimeoutMillis),
                Duration.ofMillis(testTimeoutMillis),
                permRestrictions,
                dependenciesStrings.stream().map(Path::of).collect(toList()));
    }

    @JsonProperty
    public String testClassName() {
        return testClassName;
    }

    @JsonProperty
    public String codeUnderTestString() {
        return codeUnderTest.toString();
    }

    public Path codeUnderTest() {
        return codeUnderTest;
    }

    @JsonProperty
    public String testCodeString() {
        return testCode.toString();
    }

    public Path testCode() {
        return testCode;
    }

    @JsonProperty
    public int repetitions() {
        return repetitions;
    }

    @JsonProperty
    public int repTimeoutMillis() {
        return (int) repTimeout.toMillis();
    }

    public Duration repTimeout() {
        return repTimeout;
    }

    @JsonProperty
    public int testTimeoutMillis() {
        return (int) testTimeout.toMillis();
    }

    public Duration testTimeout() {
        return testTimeout;
    }

    @JsonProperty
    public boolean permRestrictions() {
        return permRestrictions;
    }

    @JsonProperty
    public List<String> dependenciesStrings() {
        return dependencies.stream().map(Path::toString).collect(toList());
    }

    public List<Path> dependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof TestRunConfig)) {
            return false;
        }
        var other = (TestRunConfig) o;
        return repetitions == other.repetitions && permRestrictions == other.permRestrictions
               && repTimeout.equals(other.repTimeout) && testTimeout.equals(other.testTimeout)
               && testClassName.equals(other.testClassName) && codeUnderTest.equals(other.codeUnderTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClassName, codeUnderTest, repetitions,
                repTimeout, testTimeout, permRestrictions);
    }
}
