package ch.trick17.jtt.grader;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static java.io.File.separatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

public class Task {

    private static final Path DEFAULT_TEST_SRC_DIR = Path.of("tests").toAbsolutePath();
    private static final int DEFAULT_REPETITIONS = 7;
    private static final Duration DEFAULT_REP_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(10);

    private static final Pattern PACKAGE_NAME = Pattern.compile("\\bpackage\\s+([^\\s;]+)");
    private static final Pattern CLASS_NAME = Pattern.compile("\\bclass\\s+([^\\s{]+)");

    private final String testClassName;
    private final Map<Path, byte[]> filesToCopy;

    private Compiler compiler = ECLIPSE;
    private int repetitions = DEFAULT_REPETITIONS;
    private Duration repTimeout = DEFAULT_REP_TIMEOUT;
    private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
    private boolean permRestrictions = true;

    public static Task fromString(String testClassCode) {
        var packageName = firstMatch(testClassCode, PACKAGE_NAME);
        var simpleName = firstMatch(testClassCode, CLASS_NAME)
                .orElseThrow(() -> new IllegalArgumentException("no class name found"));
        var testClassName = concat(packageName.stream(), Stream.of(simpleName)).collect(joining("."));
        return new Task(testClassName, Map.of(toPath(testClassName), testClassCode.getBytes(UTF_8)));
    }

    private static Optional<String> firstMatch(String code, Pattern pattern) {
        return code.lines()
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .findFirst();
    }

    private static Path toPath(String className) {
        return Path.of(className.replace('.', separatorChar) + ".java");
    }

    public static Task fromClassName(String testClassName, String... moreFiles) throws IOException {
        return fromClassName(testClassName, DEFAULT_TEST_SRC_DIR, moreFiles);
    }

    public static Task fromClassName(String testClassName, Path testSrcDir,
                                     String... moreFiles) throws IOException {
        var testPath = toPath(testClassName);
        var filesToCopy = new HashMap<>(Map.of(testPath, readAllBytes(testSrcDir.resolve(testPath))));
        for (var file : moreFiles) {
            var path = Path.of(file);
            filesToCopy.put(path, readAllBytes(testSrcDir.resolve(path)));
        }
        return new Task(testClassName, filesToCopy);
    }

    private Task(String testClassName, Map<Path, byte[]> filesToCopy) {
        this.testClassName = testClassName;
        this.filesToCopy = filesToCopy;
    }

    /**
     * Sets to compiler to use. The default is {@link Compiler#ECLIPSE}
     */
    public Task compiler(Compiler compiler) {
        this.compiler = compiler;
        return this;
    }

    /**
     * Defines the number of times a single test is executed ("repeated"). Must
     * be at least 1.
     */
    public Task repetitions(int repetitions) {
        if (repetitions < 1) {
            throw new IllegalArgumentException();
        }
        this.repetitions = repetitions;
        return this;
    }

    /**
     * Sets the timeouts to be used for test execution. The repetition timeout
     * is a hard timeout per execution ("repetition") of each test, while the
     * test timeout concerns all repetitions of a single test together; once the
     * test timeout has been reached, no more repetitions are attempted, but the
     * currently running repetition continues to execute, until it is finished
     * or the repetition timeout is reached.
     */
    public Task timeouts(Duration repTimeout, Duration testTimeout) {
        if (repTimeout.isNegative() || repTimeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (testTimeout.compareTo(repTimeout) < 0) {
            throw new IllegalArgumentException("test timeout must be >= repetition timeout");
        }
        this.repTimeout = repTimeout;
        this.testTimeout = testTimeout;
        return this;
    }

    public Task permRestrictions(boolean permRestrictions) {
        this.permRestrictions = permRestrictions;
        return this;
    }

    public String testClassName() {
        return testClassName;
    }

    public String testClassSimpleName() {
        var parts = testClassName.split("\\.");
        return parts[parts.length - 1];
    }

    public Map<Path, byte[]> filesToCopy() {
        return unmodifiableMap(filesToCopy);
    }

    public Compiler compiler() {
        return compiler;
    }

    public int repetitions() {
        return repetitions;
    }

    public Duration repTimeout() {
        return repTimeout;
    }

    public Duration testTimeout() {
        return testTimeout;
    }

    public boolean permRestrictions() {
        return permRestrictions;
    }
}
