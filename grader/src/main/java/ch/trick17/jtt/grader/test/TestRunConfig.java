package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class TestRunConfig {

    // good candidate for a record, once we want to update to that Java version

    private static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private final String testClassName;
    private final List<Path> codeUnderTest = new ArrayList<>();
    private final int repetitions;
    private final Duration repTimeout;
    private final Duration testTimeout;
    private final boolean permRestrictions;
    private final List<Path> dependencies = new ArrayList<>();

    public TestRunConfig(
            String testClassName,
            List<Path> codeUnderTest,
            int repetitions,
            Duration repTimeout,
            Duration testTimeout,
            boolean permRestrictions,
            List<Path> dependencies) {
        this.testClassName = requireNonNull(testClassName);
        this.codeUnderTest.addAll(codeUnderTest);
        this.repetitions = repetitions;
        this.repTimeout = requireNonNull(repTimeout);
        this.testTimeout = requireNonNull(testTimeout);
        this.permRestrictions = permRestrictions;
        this.dependencies.addAll(dependencies);
    }

    @JsonCreator
    public TestRunConfig(
            String testClassName,
            List<String> codeUnderTestStrings,
            int repetitions,
            int repTimeoutMillis,
            int testTimeoutMillis,
            boolean permRestrictions,
            List<String> dependenciesStrings) {
        this(testClassName,
                codeUnderTestStrings.stream().map(Path::of).collect(toList()),
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
    public List<String> codeUnderTestStrings() {
        return codeUnderTest.stream().map(Path::toString).collect(toList());
    }

    public List<Path> codeUnderTest() {
        return codeUnderTest;
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

    public static TestRunConfig fromJson(String json) throws JsonProcessingException {
        return mapper.readValue(json, TestRunConfig.class);
    }

    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
