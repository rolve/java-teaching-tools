package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.testrunner.TestMethod;

import java.util.List;
import java.util.stream.Stream;

public record GradeResult(
        boolean compiled,
        boolean emptyTestSuite,
        List<RefImplementationResult> refImplementationResults,
        List<MutantResult> mutantResults,
        Double refImplementationScore, Double mutantScore, Double totalScore) {

    public GradeResult {
        Stream.of(refImplementationScore, mutantScore, totalScore).forEach(score -> {
            if (score != null && (score < 0.0 || score > 1.0)) {
                throw new IllegalArgumentException("invalid score: " + score);
            }
        });
    }

    public record RefImplementationResult(
            List<TestMethod> failedTests) {
        public boolean passed() {
            return failedTests.isEmpty();
        }
    }

    public record MutantResult(
            Mutation mutation,
            List<TestMethod> failedTests) {
        public boolean passed() {
            return failedTests.isEmpty();
        }
    }
}
