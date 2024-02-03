package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemClassFile;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.List;

public record Mutant(
        List<InMemClassFile> classes,
        int mutatedClassIndex,
        MutationDetails mutation) {

    public String getDescription() {
        return mutation.getDescription()
               + " at " + classes.get(mutatedClassIndex).getSimpleName()
               + ":" + mutation.getLineNumber();
    }
}
