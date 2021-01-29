package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public class TestResult {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final List<MethodResult> methodResults;

    public TestResult(
            @JsonProperty("methodResults") List<MethodResult> methodResults) {
        this.methodResults = copyOf(methodResults);
    }

    @JsonProperty
    public List<MethodResult> methodResults() {
        return methodResults;
    }

    public static class MethodResult {
        // TODO: make this a record at some point

        private final String method;
        private final boolean passed;
        private final List<String> failMsgs;
        private final boolean nonDeterm;
        private final int repsMade;
        private final boolean timeout;
        private final List<String> illegalOps;

        public MethodResult(
                @JsonProperty("method") String method,
                @JsonProperty("passed") boolean passed,
                @JsonProperty("failMsgs") List<String> failMsgs,
                @JsonProperty("nonDeterm") boolean nonDeterm,
                @JsonProperty("repsMade") int repsMade,
                @JsonProperty("timeout") boolean timeout,
                @JsonProperty("illegalOps") List<String> illegalOps) {
            this.method = requireNonNull(method);
            this.passed = passed;
            this.failMsgs = copyOf(failMsgs);
            this.nonDeterm = nonDeterm;
            this.repsMade = repsMade;
            this.timeout = timeout;
            this.illegalOps = copyOf(illegalOps);
        }

        @JsonProperty
        public String method() {
            return method;
        }

        @JsonProperty
        public boolean passed() {
            return passed;
        }

        @JsonProperty
        public List<String> failMsgs() {
            return failMsgs;
        }

        @JsonProperty
        public boolean nonDeterm() {
            return nonDeterm;
        }

        @JsonProperty
        public int repsMade() {
            return repsMade;
        }

        @JsonProperty
        public boolean timeout() {
            return timeout;
        }

        @JsonProperty
        public List<String> illegalOps() {
            return illegalOps;
        }
    }

    public static TestResult fromJson(String json) throws JsonProcessingException {
        return mapper.readValue(json, TestResult.class);
    }

    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
