package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;

import java.time.Duration;
import java.util.List;

public record TestRunConfig(
        String testClassName,
        ClassPath sandboxedCode,
        ClassPath supportCode,
        int repetitions,
        Duration repTimeout,
        Duration testTimeout,
        String permittedCalls,
        List<String> vmArgs) {
}
