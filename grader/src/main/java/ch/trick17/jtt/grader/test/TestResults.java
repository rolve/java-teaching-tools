package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public class TestResults implements Iterable<TestResults.MethodResult> {

    private final List<MethodResult> methodResults;

    @JsonCreator
    public TestResults(List<MethodResult> methodResults) {
        this.methodResults = copyOf(methodResults);
    }

    @JsonProperty
    public List<MethodResult> methodResults() {
        return methodResults;
    }

    public Optional<MethodResult> methodResultFor(String method) {
        return methodResults.stream()
                .filter(r -> r.method.equals(method))
                .findFirst();
    }

    @Override
    public Iterator<MethodResult> iterator() {
        return methodResults.iterator();
    }

    public Stream<MethodResult> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public static class MethodResult {
        // TODO: make this a record at some point

        private final String method;
        private final boolean passed;
        private final List<String> failMsgs;
        private final boolean nonDeterm;
        private final int repsMade;
        private final boolean incompleteReps;
        private final boolean timeout;
        private final boolean outOfMemory;
        private final List<String> illegalOps;
        private final List<Double> scores;

        @JsonCreator
        public MethodResult(
                String method,
                boolean passed,
                Collection<String> failMsgs,
                boolean nonDeterm,
                int repsMade,
                boolean incompleteReps,
                boolean timeout,
                boolean outOfMemory,
                List<String> illegalOps,
                List<Double> scores) {
            this.method = requireNonNull(method);
            this.passed = passed;
            this.failMsgs = copyOf(failMsgs);
            this.nonDeterm = nonDeterm;
            this.repsMade = repsMade;
            this.incompleteReps = incompleteReps;
            this.timeout = timeout;
            this.outOfMemory = outOfMemory;
            this.illegalOps = copyOf(illegalOps);
            this.scores = copyOf(scores);
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
        public boolean incompleteReps() {
            return incompleteReps;
        }

        @JsonProperty
        public boolean timeout() {
            return timeout;
        }

        @JsonProperty
        public boolean outOfMemory() {
            return outOfMemory;
        }

        @JsonProperty
        public List<String> illegalOps() {
            return illegalOps;
        }

        @JsonProperty
        public List<Double> scores() {
            return scores;
        }
    }
}
