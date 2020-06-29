package ch.trick17.jtt.grader;

import static ch.trick17.jtt.grader.result.Property.*;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

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

    // Timeout per test execution. After the timeout, the test is killed forcibly.
    private static final long EXEC_TIMEOUT = 6000; // ms

    // For how long we do REPETITIONS (over all tests). If we were running tests
    // for more than this time, do not attempt another repetition.
    private static final long TEST_TIMEOUT = 10000; // ms

    private static PrintStream out;
    private static PrintStream err;

    public static void main(String[] args) throws Exception {
        var testClass = args[0];
        runTests(testClass);
    }

    private static void runTests(String testClass)
            throws Exception {
        // Close standard input in case some solutions read from it
        System.in.close();

        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(nullOutputStream()));
        System.setErr(new PrintStream(nullOutputStream()));

        var passed = new HashSet<String>();
        var failed = new HashSet<String>();
        var failMsgs = new TreeSet<String>();

        var methods = findTestMethods(testClass);

        var startTime = currentTimeMillis();
        for (int rep = 0; rep < REPETITIONS; rep++) {
            for (var method : methods) {
                var result = runTest(method);

                var name = method.getMethodName();
                if (result.getStatus() == SUCCESSFUL) {
                    passed.add(name);
                } else {
                    failed.add(name);
                    var exc = result.getThrowable().get();
                    var msg = name + ": "
                            + valueOf(exc.getMessage()).replaceAll("\\s+", " ")
                            + " (" + exc.getClass().getName() + ")";
                    // TODO: Collect exception stats
                    failMsgs.add(msg);
                    if (result.getThrowable().get() instanceof ThreadDeath) {
                        out.println("prop: " + TIMEOUT);
                    }
                }
            }

            if (rep > 0 && rep < REPETITIONS - 1
                    && currentTimeMillis() - startTime > TEST_TIMEOUT) {
                // this timeout is not so bad. It just means that we are not so
                // sure that the tests are deterministic, since not all
                // REPETITIONS were tried
                err.println("Only " + rep + " repetitions made");
                out.println("prop: " + INCOMPLETE_REPETITIONS);
                break;
            }
        }

        failMsgs.stream()
                .flatMap(s -> stream(s.split("\n")))
                .map("    "::concat)
                .forEach(err::println);

        var nonDeterm = new HashSet<>(passed);
        nonDeterm.retainAll(failed);
        if (!nonDeterm.isEmpty()) {
            err.println("Non-determinism detected in tests: " + nonDeterm);
            out.println("prop: " + NONDETERMINISTIC);
        }

        passed.removeAll(nonDeterm);
        passed.forEach(t -> out.println("test: " + t));
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
                thread.join(EXEC_TIMEOUT); // ja ja, should calculate time left
                break;
            } catch (InterruptedException e) {}
        }
        if (thread.isAlive()) {
            stop(thread);
        }

        if (result.get() == null) { // quite unlikely but possible, I suppose
            return failed(new ThreadDeath());
        } else {
            return result.get();
        }
    }

    @SuppressWarnings("deprecation")
    private static void stop(Thread thread) {
        thread.stop(); // <- badass
    }
}
