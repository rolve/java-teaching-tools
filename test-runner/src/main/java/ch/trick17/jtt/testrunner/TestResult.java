package ch.trick17.jtt.testrunner;

import java.util.List;

public record TestResult(
        TestMethod method,
        boolean passed,
        List<Throwable> exceptions,
        boolean nonDeterm,
        int repsMade,
        boolean incompleteReps,
        boolean timeout,
        boolean outOfMemory,
        List<String> illegalOps,
        List<Double> scores) {
}
