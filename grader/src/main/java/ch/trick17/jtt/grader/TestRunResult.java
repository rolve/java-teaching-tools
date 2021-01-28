package ch.trick17.jtt.grader;

import java.util.List;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public class TestRunResult {

    private final List<MethodResult> methodResults;

    public TestRunResult(List<MethodResult> methodResults) {
        this.methodResults = copyOf(methodResults);
    }

    public List<MethodResult> methodResults() {
        return methodResults;
    }

    public static class MethodResult {
        // TODO: make this a record at some point

        private final String methodName;
        private final boolean passed;
        private final List<String> failMsgs;
        private final boolean nonDeterm;
        private final int repsMade;
        private final boolean timeout;
        private final List<String> illegalOps;

        public MethodResult(String methodName, boolean passed, List<String> failMsgs, boolean nonDeterm,
                            int repsMade, boolean timeout, List<String> illegalOps) {
            this.methodName = requireNonNull(methodName);
            this.passed = passed;
            this.failMsgs = copyOf(failMsgs);
            this.nonDeterm = nonDeterm;
            this.repsMade = repsMade;
            this.timeout = timeout;
            this.illegalOps = copyOf(illegalOps);
        }

        public String method() {
            return methodName;
        }

        public boolean passed() {
            return passed;
        }

        public List<String> failMsgs() {
            return failMsgs;
        }

        public boolean nonDeterm() {
            return nonDeterm;
        }

        public int repsMade() {
            return repsMade;
        }

        public boolean timeout() {
            return timeout;
        }

        public List<String> illegalOps() {
            return illegalOps;
        }
    }
}
