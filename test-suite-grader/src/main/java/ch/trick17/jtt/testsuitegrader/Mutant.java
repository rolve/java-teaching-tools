package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemClassFile;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.List;

public record Mutant(
        int refImplementationIndex,
        int mutatedClassIndex,
        MutationDetails details,
        List<InMemClassFile> classes) {

    public String getDescription() {
        return details.getDescription()
               + " at " + details.getFilename()
               + ":" + details.getLineNumber();
    }
}
