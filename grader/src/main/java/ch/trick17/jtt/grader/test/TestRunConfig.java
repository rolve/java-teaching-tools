package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class TestRunConfig {

    // good candidate for a record, once we want to update to that Java version

    private static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private final String testClassName;
    private final List<String> codeUnderTestPaths;
    private final int repetitions;
    private final Duration repTimeout;
    private final Duration testTimeout;
    private final boolean permRestrictions;

    public TestRunConfig(
            String testClassName,
            List<String> codeUnderTestPaths,
            int repetitions,
            Duration repTimeout,
            Duration testTimeout,
            boolean permRestrictions) {
        this.testClassName = requireNonNull(testClassName);
        this.codeUnderTestPaths = requireNonNull(codeUnderTestPaths);
        this.repetitions = repetitions;
        this.repTimeout = requireNonNull(repTimeout);
        this.testTimeout = requireNonNull(testTimeout);
        this.permRestrictions = permRestrictions;
    }

    @JsonCreator
    public TestRunConfig(
            String testClassName,
            List<String> codeUnderTestPaths,
            int repetitions,
            int repTimeoutMillis,
            int testTimeoutMillis,
            boolean permRestrictions) {
        this(testClassName, codeUnderTestPaths, repetitions,
                Duration.ofMillis(repTimeoutMillis), Duration.ofMillis(testTimeoutMillis),
                permRestrictions);
    }

    @JsonProperty
    public String testClassName() {
        return testClassName;
    }

    @JsonProperty
    public List<String> codeUnderTestPaths() {
        return codeUnderTestPaths;
    }

    public List<URL> codeUnderTest() {
        return codeUnderTestPaths.stream().map(s -> {
            try {
                return Path.of(s).toUri().toURL();
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }).collect(toList());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestRunConfig)) return false;
        var that = (TestRunConfig) o;
        return repetitions == that.repetitions && permRestrictions == that.permRestrictions
                && repTimeout.equals(that.repTimeout)&& testTimeout.equals(that.testTimeout)
                && testClassName.equals(that.testClassName) && codeUnderTestPaths.equals(that.codeUnderTestPaths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClassName, codeUnderTestPaths, repetitions,
                repTimeout, testTimeout, permRestrictions);
    }

    public static TestRunConfig fromJson(String json) throws JsonProcessingException {
        return mapper.readValue(json, TestRunConfig.class);
    }

    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
