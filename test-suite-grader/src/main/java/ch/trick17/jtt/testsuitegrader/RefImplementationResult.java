package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.testrunner.TestMethod;

import java.util.List;

public record RefImplementationResult(
        List<TestMethod> failedTests) {
    public boolean passed() {
        return failedTests.isEmpty();
    }
}
