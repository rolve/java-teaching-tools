package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassLoader;
import ch.trick17.jtt.sandbox.CustomCxtClassLoaderRunner;
import ch.trick17.jtt.sandbox.Sandbox;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.forkedvm.ForkedVmClient;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.trick17.jtt.junitextensions.internal.ScoreExtension.SCORE_KEY;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.DISCARD;
import static ch.trick17.jtt.sandbox.Sandbox.Result.Kind.*;
import static java.lang.Double.parseDouble;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.slf4j.LoggerFactory.getLogger;

public class TestRunner implements Closeable {

    private static final int MAX_RUNS_PER_VM = 500;

    private static final Logger logger = getLogger(TestRunner.class);

    private final List<String> vmArgs;
    private ForkedVmClient forkedVm;
    private final AtomicInteger runs = new AtomicInteger(0);

    public TestRunner() {
        this(emptyList());
    }

    public TestRunner(List<String> vmArgs) {
        this.vmArgs = vmArgs;
    }

    public Result run(Task task) throws IOException {
        if (System.getProperties().containsKey("test-runner.noFork")) {
            return doRun(task);
        } else {
            var allVmArgs = new ArrayList<>(vmArgs);
            allVmArgs.addAll(task.vmArgs);
            // TODO: fix memory leak and get rid of this max runs workaround?
            if (forkedVm == null
                    || !forkedVm.getVmArgs().equals(allVmArgs)
                    || runs.incrementAndGet() > MAX_RUNS_PER_VM) {
                if (forkedVm != null) {
                    forkedVm.close();
                }
                logger.info("Forking test runner VM with args: {}", join(" ", allVmArgs));
                forkedVm = new ForkedVmClient(allVmArgs, List.of(TestRunnerJacksonModule.class));
                runs.set(0);
            }
            return forkedVm.runInForkedVm(TestRunner.class, "doRun", List.of(task), Result.class);
        }
    }

    @Override
    public void close() {
        if (forkedVm != null) {
            forkedVm.close();
        }
    }

    private static Result doRun(Task task) throws IOException {
        try (var sandbox = new Sandbox.Builder(task.sandboxedCode(), task.supportCode())
                .permittedCalls(task.permittedCalls() != null
                        ? Whitelist.parse(task.permittedCalls())
                        : null)
                .timeout(task.repTimeout())
                .stdInMode(EMPTY)
                .stdOutMode(DISCARD)
                .stdErrMode(DISCARD)
                .build()) {

            var methodResults = new ArrayList<TestResult>();
            for (var method : findTestMethods(task)) {
                var startTime = currentTimeMillis();

                var passed = false;
                var failed = false;
                var exceptions = new LinkedHashSet<ExceptionDescription>();
                var repsMade = task.repetitions();
                var timeout = false;
                var outOfMemory = false;
                var illegalOps = new ArrayList<String>();
                var scores = new ArrayList<Double>();
                for (int rep = 1; rep <= task.repetitions(); rep++) {
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
                        var newExceptions = (List<?>) junitResult.get("exceptions");
                        if (newExceptions.isEmpty()) {
                            passed = true;
                        } else {
                            failed = true;
                            for (var e : newExceptions) {
                                exceptions.add(ExceptionDescription.of((Throwable) e));
                            }
                        }
                        if (junitResult.get("score") != null) {
                            scores.add((Double) junitResult.get("score"));
                        }
                    }

                    if (rep < task.repetitions() &&
                        currentTimeMillis() - startTime > task.testTimeout().toMillis()) {
                        repsMade = rep;
                        break;
                    }
                }

                var nonDeterm = passed && failed;
                passed &= !nonDeterm;
                var incompleteReps = repsMade < task.repetitions();

                var params = method.getMethodParameterTypes().isEmpty()
                        ? ""
                        : "(" + method.getMethodParameterTypes() + ")";
                var testMethod = new TestMethod(method.getClassName().replace('$', '.'),
                        method.getMethodName() + params);
                methodResults.add(new TestResult(testMethod, passed, List.copyOf(exceptions), nonDeterm,
                        repsMade, incompleteReps, timeout, outOfMemory, illegalOps, scores));
            }
            return new Result(methodResults);
        }
    }

    private static List<MethodSource> findTestMethods(Task task) throws IOException {
        // To discover test classes, JUnit needs to *load* them, so we create
        // a custom class loader and set it as the "context class loader" of
        // the current thread. It delegates to the current context class loader
        // for all classes except those given by the task.
        var loader = new InMemClassLoader(task.sandboxedCode().with(task.supportCode()),
                currentThread().getContextClassLoader());
        try (var runner = new CustomCxtClassLoaderRunner(loader)) {
            return runner.run(() -> {
                var launcher = LauncherFactory.create();
                var selectors = task.testClassNames().stream()
                        .map(c -> selectClass(c))
                        .toList();
                var classesReq = request()
                        .configurationParameters(Map.of(
                                "junit.jupiter.testclass.order.default",
                                "ch.trick17.jtt.testrunner.OrderAnnotationThenDisplayName",
                                "junit.jupiter.testmethod.order.default",
                                "ch.trick17.jtt.testrunner.OrderAnnotationThenDisplayName"))
                        .selectors(selectors);
                var testPlan = launcher.discover(classesReq.build());
                return testPlan.getRoots().stream()
                        .flatMap(id -> testPlan.getDescendants(id).stream())
                        .flatMap(id -> id.getSource().stream())
                        .filter(s -> s instanceof MethodSource)
                        .map(s -> (MethodSource) s)
                        .toList();
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static Sandbox.Result<Map<String, Object>> runSandboxed(
            MethodSource test, Sandbox sandbox) {
        var args = List.of(test.getClassName(), test.getMethodName(), test.getMethodParameterTypes());
        var result = sandbox.run(Sandboxed.class, "run",
                List.of(String.class, String.class, String.class), args, Map.class);
        return (Sandbox.Result<Map<String, Object>>) (Object) result;
    }

    public static class Sandboxed {
        public static Map<String, Object> run(String className, String methodName, String paramTypes) {
            var sel = selectMethod(className, methodName, paramTypes);
            var req = request().selectors(sel).build();
            var listener = new TestExecutionListener() {
                final List<Throwable> exceptions = new ArrayList<>();
                Double score;

                public void reportingEntryPublished(TestIdentifier id, ReportEntry entry) {
                    // TODO: how to handle scores for parameterized tests?
                    entry.getKeyValuePairs().entrySet().stream()
                            .filter(e -> e.getKey().equals(SCORE_KEY))
                            .map(e -> parseDouble(e.getValue()))
                            .findFirst()
                            .ifPresent(s -> score = s);
                }

                public void executionFinished(TestIdentifier id, TestExecutionResult result) {
                    // if the test method is parameterized, this is called multiple times
                    if (result.getThrowable().isPresent()) {
                        exceptions.add(result.getThrowable().get());
                    }
                }
            };
            LauncherFactory.create().execute(req, listener);

            // since JUnit catches the SecurityException, need to rethrow it
            // for the sandbox to record the illegal operation...
            for (var e : listener.exceptions) {
                if (e instanceof SecurityException s) {
                    throw s;
                }
            }

            // can only transfer classes loaded by the bootstrap class loader
            // across sandbox boundary...
            var result = new HashMap<String, Object>();
            result.put("exceptions", listener.exceptions);
            result.put("score", listener.score);
            return result;
        }
    }

    public record Task(
            List<String> testClassNames,
            ClassPath sandboxedCode,
            ClassPath supportCode,
            int repetitions,
            Duration repTimeout,
            Duration testTimeout,
            String permittedCalls,
            List<String> vmArgs) {

        public Task(String testClassName,
                    ClassPath sandboxedCode,
                    ClassPath supportCode) {
            this(List.of(testClassName), sandboxedCode, supportCode);
        }

        public Task(List<String> testClassNames,
                    ClassPath sandboxedCode,
                    ClassPath supportCode) {
            this(testClassNames, sandboxedCode, supportCode,
                    1, Duration.ofSeconds(1), Duration.ofSeconds(1),
                    null, emptyList());
        }
    }

    public record Result(List<TestResult> testResults) {}
}
