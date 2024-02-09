package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemSource;

import java.util.List;

public record Submission(List<InMemSource> testSuite) {
    public Submission {
        if (testSuite.isEmpty()) {
            throw new IllegalArgumentException("empty test suite");
        }
    }
}
