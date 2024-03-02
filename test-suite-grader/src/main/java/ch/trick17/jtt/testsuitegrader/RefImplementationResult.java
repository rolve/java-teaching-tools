package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.testrunner.TestMethod;

import java.util.List;
import java.util.Map;

public record RefImplementationResult(
        Map<TestMethod, List<Throwable>> failedTests) {
    public boolean passed() {
        return failedTests.isEmpty();
    }
}
