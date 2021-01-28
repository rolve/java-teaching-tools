package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.TestRunResult.MethodResult;
import ch.trick17.jtt.sandbox.CustomCxtClassLoaderRunner;
import ch.trick17.jtt.sandbox.InJvmSandbox;
import ch.trick17.jtt.sandbox.SandboxResult;
import ch.trick17.jtt.sandbox.SandboxResult.Kind;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ch.trick17.jtt.grader.result.Property.*;
import static java.io.File.pathSeparator;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.Boolean.getBoolean;
import static java.lang.Integer.getInteger;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

public class TestRunner {

    public static final String REPETITIONS_PROP = "ch.trick17.jtt.repetitions";
    public static final String REP_TIMEOUT_PROP = "ch.trick17.jtt.repTimeout";
    public static final String TEST_TIMEOUT_PROP = "ch.trick17.jtt.testTimeout";
    public static final String PERM_RESTRICTIONS_PROP = "ch.trick17.jtt.permRestrictions";

    private static final int REPETITIONS = getInteger(REPETITIONS_PROP);
    private static final int REP_TIMEOUT = getInteger(REP_TIMEOUT_PROP);
    private static final int TEST_TIMEOUT = getInteger(TEST_TIMEOUT_PROP);
    private static final boolean PERM_RESTRICTIONS = getBoolean(PERM_RESTRICTIONS_PROP);

    private static PrintStream out;
    private static PrintStream err;

    public static void main(String[] args) throws IOException {
        var testClass = args[0];
        var codeUnderTest = new ArrayList<URL>();
        for (int i = 1; i < args.length; i++) {
            codeUnderTest.add(Path.of(args[i]).toUri().toURL());
        }

        var result = runTests(testClass, codeUnderTest);

        report(result);
    }

    private static TestRunResult runTests(String testClass, List<URL> codeUnderTest)
            throws IOException {
        // Close standard input in case some solutions read from it
        System.in.close();

        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(nullOutputStream()));
        System.setErr(new PrintStream(nullOutputStream()));
        currentThread().setUncaughtExceptionHandler((t, e) -> e.printStackTrace(err));

        var methodResults = new ArrayList<MethodResult>();
        for (var method : findTestMethods(testClass, codeUnderTest)) {
            var startTime = currentTimeMillis();

            var passed = false;
            var failed = false;
            var failMsgs = new ArrayList<String>();
            var repsMade = REPETITIONS;
            var timeout = false;
            var illegalOps = new ArrayList<String>();
            for (int rep = 1; rep <= REPETITIONS; rep++) {
                var methodResult = runSandboxed(method, codeUnderTest);

                if (methodResult.kind() == Kind.TIMEOUT) {
                    timeout = true;
                } else if (methodResult.kind() == Kind.ILLEGAL_OPERATION) {
                    illegalOps.add(methodResult.exception().getMessage());
                } else if (methodResult.kind() == Kind.EXCEPTION) {
                    // should not happen, JUnit catches exceptions
                    throw new AssertionError(methodResult.exception());
                } else if (methodResult.value() == null) {
                    passed = true;
                } else {
                    failed = true;
                    var exception = methodResult.value();
                    var msg = valueOf(exception.getMessage()).replaceAll("\\s+", " ")
                            + " (" + exception.getClass().getName() + ")";
                    // TODO: Collect exception stats
                    failMsgs.add(msg);
                }

                if (rep < REPETITIONS && currentTimeMillis() - startTime > TEST_TIMEOUT) {
                    repsMade = rep;
                    break;
                }
            }

            var nonDeterm = passed && failed;
            passed &= !nonDeterm;
            methodResults.add(new MethodResult(method.getMethodName(), passed,
                    failMsgs, nonDeterm, repsMade, timeout, illegalOps));
        }

        return new TestRunResult(methodResults);
    }

    private static List<MethodSource> findTestMethods(String testClass, List<URL> codeUnderTest) {
        var urls = concat(codeUnderTest.stream(), classpathUrls().stream())
                .toArray(URL[]::new);
        var loader = new URLClassLoader(urls, currentThread().getContextClassLoader());
        return new CustomCxtClassLoaderRunner(loader).run(() -> {
            var launcher = LauncherFactory.create();
            var classReq = request().selectors(selectClass(testClass));
            var testPlan = launcher.discover(classReq.build());
            return testPlan.getRoots().stream()
                    .flatMap(id -> testPlan.getDescendants(id).stream())
                    .filter(id -> id.getType() == TEST)
                    .map(id -> (MethodSource) id.getSource().get())
                    .collect(toList());
        });
    }

    private static List<URL> classpathUrls() {
        return stream(System.getProperty("java.class.path").split(pathSeparator))
                .map(path -> {
                    try {
                        return Path.of(path).toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                })
                .collect(toList());
    }

    private static SandboxResult<Throwable> runSandboxed(MethodSource test, List<URL> codeUnderTest) {
        var sandbox = new InJvmSandbox()
                .permRestrictions(PERM_RESTRICTIONS)
                .timeout(Duration.ofMillis(REP_TIMEOUT));
        var args = List.of(test.getClassName(), test.getMethodName());
        return sandbox.run(codeUnderTest, classpathUrls(),
                Sandboxed.class, "run", List.of(String.class, String.class), args);
    }

    public static class Sandboxed {
        public static Throwable run(String className, String methodName) {
            var sel = selectMethod(className, methodName);
            var req = request().selectors(sel).build();
            var listener = new TestExecutionListener() {
                TestExecutionResult result;
                public void executionFinished(TestIdentifier id, TestExecutionResult res) {
                    if (id.isTest()) {
                        result = res;
                    }
                }
            };
            LauncherFactory.create().execute(req, listener);

            var throwable = listener.result.getThrowable().orElse(null);
            // since JUnit catches the SecurityException, need to rethrow it
            // for the sandbox to record the illegal operation...
            if (throwable instanceof SecurityException) {
                throw (SecurityException) throwable;
            }
            return throwable;
        }
    }

    private static void report(TestRunResult result) {
        for (var res : result.methodResults()) {
            if (res.passed()) {
                out.println("test: " + res.method());
            }
            res.failMsgs().stream()
                    .flatMap(s -> stream(s.split("\n")))
                    .map("    "::concat)
                    .forEach(err::println);
            if (res.nonDeterm()) {
                err.println("Non-determinism in " + res.method());
                out.println("prop: " + NONDETERMINISTIC);
            }
            if (res.repsMade() < REPETITIONS) {
                err.println("Only " + res.repsMade() + " repetitions made in " + res.method());
                out.println("prop: " + INCOMPLETE_REPETITIONS);
            }
            if (res.timeout()) {
                err.println("Timeout in " + res.method());
                out.println("prop: " + TIMEOUT);
            }
            if (!res.illegalOps().isEmpty()) {
                err.println("Illegal operation(s) in " + res.method() + ": " +
                        res.illegalOps().stream().collect(Collectors.joining(", ")));
                out.println("prop: " + ILLEGAL_OPERATION);
            }
        }
        out.flush();
        err.flush();
    }
}
