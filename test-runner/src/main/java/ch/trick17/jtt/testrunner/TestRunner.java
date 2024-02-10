package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.InMemClassLoader;
import ch.trick17.jtt.sandbox.CustomCxtClassLoaderRunner;
import ch.trick17.jtt.sandbox.Sandbox;
import ch.trick17.jtt.sandbox.SandboxResult;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.forkedvm.ForkedVmClient;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.trick17.jtt.junitextensions.internal.ScoreExtension.SCORE_KEY;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.DISCARD;
import static ch.trick17.jtt.sandbox.SandboxResult.Kind.*;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.Double.parseDouble;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

public class TestRunner implements Closeable {

    private ForkedVmClient forkedVm;

    public List<TestResult> run(TestRunConfig config) throws IOException {
        if (System.getProperties().containsKey("test-runner.noFork")) {
            return doRun(config).results;
        } else {
            if (forkedVm == null || !forkedVm.getVmArgs().equals(config.vmArgs())) {
                if (forkedVm != null) {
                    forkedVm.close();
                }
                forkedVm = new ForkedVmClient(config.vmArgs());
            }
            return forkedVm.runInForkedVm(TestRunner.class, "doRun",
                    List.of(config), TestResults.class).results;
        }
    }

    @Override
    public void close() {
        if (forkedVm != null) {
            forkedVm.close();
        }
    }

    private static TestResults doRun(TestRunConfig config) throws IOException {
        try (var sandbox = new Sandbox.Builder(config.sandboxedCode(), config.supportCode())
                .permittedCalls(config.permittedCalls() != null
                        ? Whitelist.parse(config.permittedCalls())
                        : null)
                .timeout(config.repTimeout())
                .stdInMode(EMPTY)
                .stdOutMode(DISCARD)
                .stdErrMode(DISCARD)
                .build()) {

            var methodResults = new ArrayList<TestResult>();
            for (var method : findTestMethods(config)) {
                var startTime = currentTimeMillis();

                var passed = false;
                var failed = false;
                var exceptions = new ArrayList<Throwable>();
                var repsMade = config.repetitions();
                var timeout = false;
                var outOfMemory = false;
                var illegalOps = new ArrayList<String>();
                var scores = new ArrayList<Double>();
                for (int rep = 1; rep <= config.repetitions(); rep++) {
                    var result = runSandboxed(method, sandbox);

                    if (result.kind() == TIMEOUT) {
                        timeout = true;
                        failed = true;
                    } else if (result.kind() == OUT_OF_MEMORY) {
                        outOfMemory = true;
                        failed = true;
                    } else if (result.kind() == ILLEGAL_OPERATION) {
                        illegalOps.add(result.exception().getMessage());
                        failed = true;
                    } else if (result.kind() == EXCEPTION) {
                        // does not happen for normal test exceptions, only
                        // for issues with JUnit or the sandbox itself
                        var m = method.getClassName() + "." + method.getMethodName();
                        throw new TestRunException("failed to run " + m,
                                result.exception());
                    } else {
                        var junitResult = result.value();
                        if (junitResult.get("exception") == null) {
                            passed = true;
                        } else {
                            failed = true;
                            exceptions.add((Throwable) junitResult.get("exception"));
                        }
                        if (junitResult.get("score") != null) {
                            scores.add((Double) junitResult.get("score"));
                        }
                    }

                    if (rep < config.repetitions() &&
                        currentTimeMillis() - startTime > config.testTimeout().toMillis()) {
                        repsMade = rep;
                        break;
                    }
                }

                var nonDeterm = passed && failed;
                passed &= !nonDeterm;
                var incompleteReps = repsMade < config.repetitions();

                var testMethod = new TestMethod(method.getClassName().replace('$', '.'),
                        method.getMethodName());
                methodResults.add(new TestResult(testMethod, passed, exceptions, nonDeterm,
                        repsMade, incompleteReps, timeout, outOfMemory, illegalOps, scores));

                // workaround for NoClassDefFoundError when serializing some
                // exceptions after the sandbox and the associated class loaders
                // have been closed:
                if (!exceptions.isEmpty()) {
                    try {
                        var out = new ObjectOutputStream(nullOutputStream());
                        out.writeObject(exceptions);
                    } catch (NotSerializableException ignored) {}
                }
            }
            return new TestResults(methodResults);
        }
    }

    private static List<MethodSource> findTestMethods(TestRunConfig config) throws IOException {
        // To discover test classes, JUnit needs to *load* them, so we create
        // a custom class loader and set it as the "context class loader" of
        // the current thread. It delegates to the current context class loader
        // for all classes except those given by the test run config.
        var loader = new InMemClassLoader(config.sandboxedCode().with(config.supportCode()),
                currentThread().getContextClassLoader());
        try (var runner = new CustomCxtClassLoaderRunner(loader)) {
            return runner.run(() -> {
                var launcher = LauncherFactory.create();
                var selectors = config.testClassNames().stream()
                        .map(c -> selectClass(c))
                        .toList();
                var classesReq = request()
                        .configurationParameter(
                                "junit.jupiter.testmethod.order.default",
                                "org.junit.jupiter.api.MethodOrderer$DisplayName")
                        .selectors(selectors);
                var testPlan = launcher.discover(classesReq.build());
                return testPlan.getRoots().stream()
                        .flatMap(id -> testPlan.getDescendants(id).stream())
                        .filter(id -> id.getType() == TEST)
                        .map(id -> (MethodSource) id.getSource().orElseThrow())
                        .toList();
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static SandboxResult<Map<String, Object>> runSandboxed(
            MethodSource test, Sandbox sandbox) {
        var args = List.of(test.getClassName(), test.getMethodName());
        var result = sandbox.run(Sandboxed.class, "run",
                List.of(String.class, String.class), args, Map.class);
        return (SandboxResult<Map<String, Object>>) (Object) result;
    }

    public static class Sandboxed {
        public static Map<String, Object> run(String className, String methodName) {
            var sel = selectMethod(className, methodName);
            var req = request().selectors(sel).build();
            var listener = new TestExecutionListener() {
                TestExecutionResult result;
                Double score;

                public void reportingEntryPublished(TestIdentifier id, ReportEntry entry) {
                    entry.getKeyValuePairs().entrySet().stream()
                            .filter(e -> e.getKey().equals(SCORE_KEY))
                            .map(e -> parseDouble(e.getValue()))
                            .findFirst()
                            .ifPresent(s -> score = s);
                }

                public void executionFinished(TestIdentifier id, TestExecutionResult res) {
                    if (id.isTest()) {
                        result = res;
                    }
                }
            };
            LauncherFactory.create().execute(req, listener);

            var exception = listener.result == null
                    ? null // test was skipped
                    : listener.result.getThrowable().orElse(null);

            // since JUnit catches the SecurityException, need to rethrow it
            // for the sandbox to record the illegal operation...
            if (exception instanceof SecurityException) {
                throw (SecurityException) exception;
            }

            // can only transfer classes loaded by the bootstrap class loader
            // across sandbox boundary...
            var result = new HashMap<String, Object>();
            result.put("exception", exception);
            result.put("score", listener.score);
            return result;
        }
    }

    private record TestResults(List<TestResult> results) {}
}
