package ch.trick17.jtt.grader.test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public record TestRunConfig(
        String testClassName,
        Path codeUnderTest,
        Path testCode,
        int repetitions,
        Duration repTimeout,
        Duration testTimeout,
        String permittedCalls,
        List<Path> dependencies) {
}
