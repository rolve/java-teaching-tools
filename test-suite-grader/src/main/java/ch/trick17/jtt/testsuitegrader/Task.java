package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemSource;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record Task(List<List<InMemSource>> refImplementations,
                   InMemSource refTestSuite) {
    public Task {
        if (refImplementations.isEmpty()) {
            throw new IllegalArgumentException("No reference implementations");
        }
        requireNonNull(refTestSuite);
    }

    public String testClassName() {
        return refTestSuite.getPath().replace('/', '.').replaceAll("\\.java$", "");
    }
}
