package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;

import java.time.Duration;
import java.util.List;

import static java.util.Collections.emptyList;

public record TestRunConfig(
        List<String> testClassNames,
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
        this(List.of(testClassName), sandboxedCode, supportCode);
    }

    public TestRunConfig(List<String> testClassNames,
                         ClassPath sandboxedCode,
                         ClassPath supportCode) {
        this(testClassNames, sandboxedCode, supportCode,
                1, Duration.ofSeconds(1), Duration.ofSeconds(1),
                null, emptyList());
    }
}
