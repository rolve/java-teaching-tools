package ch.trick17.jtt.testrunner;

import java.util.List;
import java.util.Optional;

public record TestResults(List<MethodResult> methodResults)  {

    /**
     * Convenience method to get the result of a specific test method, without
     * specifying the class name.
     */
    public Optional<MethodResult> methodResultFor(String method) {
        return methodResults.stream()
                .filter(r -> r.method.name().equals(method))
                .findFirst();
    }

    public record MethodResult(
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
}
