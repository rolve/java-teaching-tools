package ch.trick17.jtt.grader.test;

import ch.trick17.jtt.grader.test.TestResults.MethodResult;
import ch.trick17.jtt.sandbox.CustomCxtClassLoaderRunner;
import ch.trick17.jtt.sandbox.JavaSandbox;
import ch.trick17.jtt.sandbox.SandboxResult;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.DISCARD;
import static ch.trick17.jtt.sandbox.SandboxResult.Kind.*;
import static java.io.File.pathSeparator;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

public class TestRun {

    private final TestRunConfig config;

    public TestRun(TestRunConfig config) {
        this.config = config;
    }

    public TestResults execute() {
        var methodResults = new ArrayList<MethodResult>();
        for (var method : findTestMethods()) {
            var startTime = currentTimeMillis();

            var passed = false;
            var failed = false;
            var failMsgs = new LinkedHashSet<String>();
            var repsMade = config.repetitions();
            var timeout = false;
            var outOfMemory = false;
            var illegalOps = new ArrayList<String>();
            for (int rep = 1; rep <= config.repetitions(); rep++) {
                var methodResult = runSandboxed(method);

                if (methodResult.kind() == TIMEOUT) {
                    timeout = true;
                } else if (methodResult.kind() == OUT_OF_MEMORY) {
                    outOfMemory = true;
                } else if (methodResult.kind() == ILLEGAL_OPERATION) {
                    illegalOps.add(methodResult.exception().getMessage());
                } else if (methodResult.kind() == EXCEPTION) {
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
            methodResults.add(new MethodResult(name, passed, failMsgs, nonDeterm,
                    repsMade, repsMade < config.repetitions(), timeout, outOfMemory, illegalOps));
        }

        return new TestResults(methodResults);
    }

    private List<MethodSource> findTestMethods() {
        var urls = concat(config.codeUnderTest().stream(), classpathUrls().stream())
                .toArray(URL[]::new);
        // to discover test classes, JUnit needs to *load* them, so we create
        // a custom class loader with a classpath that includes the code under
        // test and set it as the "context class loader" of the current thread
        var loader = new URLClassLoader(urls, currentThread().getContextClassLoader());
        return new CustomCxtClassLoaderRunner(loader).run(() -> {
            var launcher = LauncherFactory.create();
            var classReq = request().selectors(selectClass(config.testClassName()));
            var testPlan = launcher.discover(classReq.build());
            return testPlan.getRoots().stream()
                    .flatMap(id -> testPlan.getDescendants(id).stream())
                    .filter(id -> id.getType() == TEST)
                    .map(id -> (MethodSource) id.getSource().orElseThrow())
                    .collect(toList());
        });
    }

    private List<URL> classpathUrls() {
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

    private SandboxResult<Throwable> runSandboxed(MethodSource test) {
        var sandbox = new JavaSandbox()
                .permRestrictions(config.permRestrictions())
                .timeout(config.repTimeout())
                .stdInMode(EMPTY).stdOutMode(DISCARD).stdErrMode(DISCARD);
        var args = List.of(test.getClassName(), test.getMethodName());
        return sandbox.run(config.codeUnderTest(), classpathUrls(), Sandboxed.class,
                "run", List.of(String.class, String.class), args, Throwable.class);
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

            var throwable = Optional.ofNullable(listener.result)
                    .flatMap(TestExecutionResult::getThrowable).orElse(null);
            // since JUnit catches the SecurityException, need to rethrow it
            // for the sandbox to record the illegal operation...
            if (throwable instanceof SecurityException) {
                throw (SecurityException) throwable;
            }
            return throwable;
        }
    }
}
