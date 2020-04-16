package javagrader;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

public class TestRunner {

    private static final int REPETITIONS = 7;

    // Timeout per test. After the timeout, the test is killed forcibly.
    private static final long TIMEOUT = 6000; // ms

    // For how long we do REPETITIONS (over all tests). If we were running tests
    // for more than this time, do not attempt another repetition.
    private static final long MAX_RUNNING_TIME = 10000; // ms

    public static PrintStream out;
    public static PrintStream err;

    public static void main(String[] args) throws Exception {
        var testClass = args[0];
        runTests(testClass);
    }

    private static void runTests(String testClass)
            throws Exception {
        // Close standard input in case some solutions read from it
        System.in.close();

        @SuppressWarnings("resource")
        PrintStream nop = new PrintStream(new OutputStream() {
            public void write(int b) {}
        });
        out = System.out;
        err = System.err;
        System.setOut(nop);
        System.setErr(nop);

        var passedSets = new HashSet<Set<String>>();
        var everFailed = new HashSet<String>();
        var failMsgs = new TreeSet<String>();

        var methods = findTestMethods(testClass);

        var startTime = currentTimeMillis();
        for (int rep = 0; rep < REPETITIONS; rep++) {
            var passed = new HashSet<String>();
            for (var method : methods) {
                var result = runTest(method);

                var name = method.getMethodName();
                if (result.getStatus() == SUCCESSFUL) {
                    passed.add(name);
                } else {
                    everFailed.add(name);
                    var exception = result.getThrowable().get();
                    var msg = name + ": " + exception.getMessage()
                            + " (" + exception.getClass().getName() + ")";
                    // TODO: Collect exception stats
                    failMsgs.add(msg);
                    if (result.getThrowable().get() instanceof ThreadDeath) {
                        out.println("timeout");
                    }
                }
            }
            passedSets.add(passed);

            if (rep > 0 && rep < REPETITIONS - 1
                    && currentTimeMillis() - startTime > MAX_RUNNING_TIME) {
                // this timeout is not so bad. It just means that we are not so
                // sure that the tests are deterministic, since not all
                // REPETITIONS were tried
                out.println("only " + rep + " repetitions");
                err.println("only " + rep + " repetitions");
                break;
            }
        }

        failMsgs.stream()
                .flatMap(s -> stream(s.split("\n")))
                .map("    "::concat)
                .forEach(err::println);

        if (passedSets.size() > 1) {
            // take the intersection of all tests that once failed tests with all
            // tests that once passed tests.
            var everPassed = passedSets.stream().flatMap(Set::stream).collect(toSet());
            var nonDeterm = new HashSet<>(everFailed);
            nonDeterm.retainAll(everPassed);

            err.println("Non-determinism detected in tests: " + nonDeterm);
            out.println("nondeterministic");
        }

        var alwaysSucc = passedSets.iterator().next();
        alwaysSucc.removeAll(everFailed);

        alwaysSucc.forEach(out::println);
        out.flush();
        err.flush();
    }

    private static List<MethodSource> findTestMethods(String testClass) {
        var launcher = LauncherFactory.create();
        var classReq = request().selectors(selectClass(testClass));
        var testPlan = launcher.discover(classReq.build());
        return testPlan.getRoots().stream()
                .flatMap(id -> testPlan.getDescendants(id).stream())
                .filter(id -> id.getType() == TEST)
                .map(id -> (MethodSource) id.getSource().get())
                .collect(toList());
    }

    /**
     * Runs test in a different thread so it can be killed after some timeout.
     * This is a very hard timeout ({@link Thread#stop()}) that should be able
     * to handle anything the students might do.
     */
    private static TestExecutionResult runTest(MethodSource m) {
        var sel = selectMethod(m.getClassName(), m.getMethodName());
        var req = request().selectors(sel).build();
        // create a new launcher, just in case the old one got corrupted
        var launcher = LauncherFactory.create();

        var result = new AtomicReference<TestExecutionResult>();
        var listener = new TestExecutionListener() {
            public void executionFinished(TestIdentifier id,
                    TestExecutionResult res) {
                if (id.isTest()) {
                    result.set(res);
                }
            }
        };

        var thread = new Thread(() -> launcher.execute(req, listener));
        thread.start();
        while (true) {
            try {
                thread.join(TIMEOUT); // ja ja, should calculate time left
                break;
            } catch (InterruptedException e) {}
        }
        if (thread.isAlive()) {
            thread.stop(); // <- badass
        }

        if (result.get() == null) { // quite unlikely but possible, I suppose
            return failed(new ThreadDeath());
        } else {
            return result.get();
        }
    }
}
