package ch.trick17.jtt.grader.test;

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

    private static final ObjectMapper mapper = new ObjectMapper();

    private final String testClass;
    private final List<String> codeUnderTestPaths;
    private final int repetitions;
    private final Duration repTimeout;
    private final Duration testTimeout;
    private final boolean permRestrictions;

    public TestRunConfig(
            String testClass,
            List<String> codeUnderTestPaths,
            int repetitions,
            Duration repTimeout,
            Duration testTimeout,
            boolean permRestrictions) {
        this.testClass = requireNonNull(testClass);
        this.codeUnderTestPaths = requireNonNull(codeUnderTestPaths);
        this.repetitions = repetitions;
        this.repTimeout = repTimeout;
        this.testTimeout = testTimeout;
        this.permRestrictions = permRestrictions;
    }

    public TestRunConfig(
            @JsonProperty("testClass") String testClass,
            @JsonProperty("codeUnderTestPaths") List<String> codeUnderTestPaths,
            @JsonProperty("repetitions") int repetitions,
            @JsonProperty("repTimeoutMillis") int repTimeout,
            @JsonProperty("testTimeoutMillis") int testTimeout,
            @JsonProperty("permRestrictions") boolean permRestrictions) {
        this(testClass, codeUnderTestPaths, repetitions,
                Duration.ofMillis(repTimeout), Duration.ofMillis(testTimeout),
                permRestrictions);
    }

    @JsonProperty
    public String testClass() {
        return testClass;
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
                && testClass.equals(that.testClass) && codeUnderTestPaths.equals(that.codeUnderTestPaths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClass, codeUnderTestPaths, repetitions,
                repTimeout, testTimeout, permRestrictions);
    }

    public static TestRunConfig fromJson(String json) {
        try {
            return mapper.readValue(json, TestRunConfig.class);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }
}
