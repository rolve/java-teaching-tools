package ch.trick17.jtt.testrunner;

import java.util.List;
import java.util.Optional;

public record TestResults(
        List<MethodResult> methodResults)  {

    public Optional<MethodResult> methodResultFor(String method) {
        return methodResults.stream()
                .filter(r -> r.method.equals(method))
                .findFirst();
    }

    public record MethodResult(
        String method,
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
