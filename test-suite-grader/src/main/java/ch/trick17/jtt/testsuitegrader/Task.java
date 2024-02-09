package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.testrunner.TestMethod;

import java.util.List;

public record Task(
        List<List<InMemClassFile>> refImplementations,
        List<TestMethod> refTests,
        List<Mutation> mutations) {

    public List<InMemClassFile> refImplementationFor(Mutation mutation) {
        return refImplementations.get(mutation.refImplementationIndex());
    }
}
