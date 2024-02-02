package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;

import java.time.Duration;
import java.util.List;

import static java.util.Collections.emptyList;

public record TestRunConfig(
        String testClassName,
        ClassPath sandboxedCode,
        ClassPath supportCode,
        int repetitions,
        Duration repTimeout,
        Duration testTimeout,
        String permittedCalls,
        List<String> vmArgs) {

    public TestRunConfig(String testClassName,
                         ClassPath sandboxedCode,
                         ClassPath supportCode) {
        this(testClassName, sandboxedCode, supportCode,
                1, Duration.ofSeconds(1), Duration.ofSeconds(1),
                null, emptyList());
    }
}
