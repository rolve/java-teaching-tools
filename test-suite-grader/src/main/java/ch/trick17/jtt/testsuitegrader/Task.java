package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.testrunner.TestMethod;

import java.util.List;
import java.util.Map;

public record Task(
        List<List<InMemClassFile>> refImplementations,
        List<Mutation> mutations,
        Map<TestMethod, String> refTestDescriptions) {

    public List<InMemClassFile> refImplementationFor(Mutation mutation) {
        return refImplementations.get(mutation.refImplementationIndex());
    }
}
