package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.testrunner.TestMethod;
import org.pitest.mutationtest.engine.MutationIdentifier;

import java.util.List;

public record Mutation(
        int refImplementationIndex,
        int mutatedClassIndex,
        MutationIdentifier identifier,
        double weight,
        List<TestMethod> killers) {}
