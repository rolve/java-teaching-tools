package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.InMemClassLoader;
import ch.trick17.jtt.sandbox.CustomCxtClassLoaderRunner;
import ch.trick17.jtt.sandbox.Sandbox;
import ch.trick17.jtt.sandbox.SandboxResult;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.TestResults.MethodResult;
import ch.trick17.jtt.testrunner.forkedvm.ForkedVmClient;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import static ch.trick17.jtt.junitextensions.internal.ScoreExtension.SCORE_KEY;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.DISCARD;
import static ch.trick17.jtt.sandbox.SandboxResult.Kind.*;
import static java.lang.Double.parseDouble;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

public class TestRunner implements Closeable {

    private ForkedVmClient forkedVm;

    public TestResults run(TestRunConfig config) throws IOException {
        if (System.getProperties().containsKey("test-runner.noFork")) {
            return doRun(config);
        } else {
            if (forkedVm == null || !forkedVm.getVmArgs().equals(config.vmArgs())) {
                if (forkedVm != null) {
                    forkedVm.close();
                }
                forkedVm = new ForkedVmClient(config.vmArgs());
            }
            return forkedVm.runInForkedVm(TestRunner.class, "doRun",
                    List.of(config), TestResults.class);
        }
    }

    @Override
    public void close() {
        if (forkedVm != null) {
            forkedVm.close();
        }
    }

    public static TestResults doRun(TestRunConfig config) throws IOException {
        try (var sandbox = new Sandbox.Builder(config.sandboxedCode(), config.supportCode())
                .permittedCalls(config.permittedCalls() != null
                        ? Whitelist.parse(config.permittedCalls())
                        : null)
                .timeout(config.repTimeout())
                .stdInMode(EMPTY)
                .stdOutMode(DISCARD)
                .stdErrMode(DISCARD)
                .build()) {

            var methodResults = new ArrayList<MethodResult>();
            for (var method : findTestMethods(config)) {
                var startTime = currentTimeMillis();

                var passed = false;
                var failed = false;
                var failMsgs = new LinkedHashSet<String>();
                var repsMade = config.repetitions();
                var timeout = false;
                var outOfMemory = false;
                var illegalOps = new ArrayList<String>();
                var scores = new ArrayList<Double>();
                for (int rep = 1; rep <= config.repetitions(); rep++) {
                    var methodResult = runSandboxed(method, sandbox);

                    if (methodResult.kind() == TIMEOUT) {
                        timeout = true;
                    } else if (methodResult.kind() == OUT_OF_MEMORY) {
                        outOfMemory = true;
                    } else if (methodResult.kind() == ILLEGAL_OPERATION) {
                        illegalOps.add(methodResult.exception().getMessage());
                    } else if (methodResult.kind() == EXCEPTION) {
                        // should not happen, JUnit catches exceptions
                        throw new AssertionError(methodResult.exception());
                    } else {
                        var junitResult = methodResult.value();
                        if (junitResult.get("throwable") == null) {
                            passed = true;
                        } else {
                            failed = true;
                            var throwable = (Throwable) junitResult.get("throwable");
                            var msg = valueOf(throwable.getMessage()).replaceAll("\\s+", " ")
                                      + " (" + throwable.getClass().getName() + ")";
                            // TODO: Collect exception stats
                            failMsgs.add(msg);
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

                var name = method.getMethodName();
                if (method.getClassName().contains("$")) { // nested test
                    var prefix = stream(method.getClassName().split("\\$"))
                            .skip(1) // skip outermost class
                            .collect(joining("."));
                    name = prefix + "." + name;
                }
                var incompleteReps = repsMade < config.repetitions();
                methodResults.add(new MethodResult(name, passed, copyOf(failMsgs), nonDeterm,
                        repsMade, incompleteReps, timeout, outOfMemory, illegalOps, scores));
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
                var classReq = request()
                        .configurationParameter("junit.jupiter.testmethod.order.default",
                                "org.junit.jupiter.api.MethodOrderer$DisplayName")
                        .selectors(selectClass(config.testClassName()));
                var testPlan = launcher.discover(classReq.build());
                return testPlan.getRoots().stream()
                        .flatMap(id -> testPlan.getDescendants(id).stream())
                        .filter(id -> id.getType() == TEST)
                        .map(id -> (MethodSource) id.getSource().orElseThrow())
                        .collect(toList());
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

            var throwable = listener.result == null
                    ? null // test was skipped
                    : listener.result.getThrowable().orElse(null);

            // since JUnit catches the SecurityException, need to rethrow it
            // for the sandbox to record the illegal operation...
            if (throwable instanceof SecurityException) {
                throw (SecurityException) throwable;
            }

            // can only transfer classes loaded by the bootstrap class loader
            // across sandbox boundary...
            var result = new HashMap<String, Object>();
            result.put("throwable", throwable);
            result.put("score", listener.score);
            return result;
        }
    }
}