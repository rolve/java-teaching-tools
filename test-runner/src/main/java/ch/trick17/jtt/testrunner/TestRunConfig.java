package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.InMemClassFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public record TestRunConfig(
        String testClassName,
        List<InMemClassFile> classes,
        List<InMemClassFile> testClasses,
        int repetitions,
        Duration repTimeout,
        Duration testTimeout,
        String permittedCalls,
        List<Path> dependencies,
        List<String> vmArgs) {
}
