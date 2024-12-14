package ch.trick17.jtt.testrunner;

import java.util.List;

public record TestResult(
        TestMethod method,
        boolean passed,
        List<ExceptionDescription> exceptions,
        boolean nonDeterm,
        int repsMade,
        boolean incompleteReps,
        boolean timeout,
        boolean outOfMemory,
        List<String> illegalOps,
        List<Double> scores) {

    public TestResult with(List<ExceptionDescription> exceptions) {
        return new TestResult(method, passed, exceptions, nonDeterm, repsMade,
                incompleteReps, timeout, outOfMemory, illegalOps, scores);
    }
}
