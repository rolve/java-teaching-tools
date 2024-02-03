package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemSource;

import java.util.List;

public record Submission(List<InMemSource> sources) {
    public Submission {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("No sources");
        }
    }
}
