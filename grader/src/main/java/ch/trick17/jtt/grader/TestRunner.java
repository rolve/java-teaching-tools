package ch.trick17.jtt.grader;

import static ch.trick17.jtt.grader.result.Property.*;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.Integer.getInteger;
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

public class TestRunner {

    public static final String REPETITIONS_PROP = "ch.trick17.jtt.repetitions";
    public static final String REP_TIMEOUT_PROP = "ch.trick17.jtt.repTimeout";
    public static final String TEST_TIMEOUT_PROP = "ch.trick17.jtt.testTimeout";

    private static final int REPETITIONS = getInteger(REPETITIONS_PROP);
    private static final int REP_TIMEOUT = getInteger(REP_TIMEOUT_PROP);
    private static final int TEST_TIMEOUT = getInteger(TEST_TIMEOUT_PROP);

    private static PrintStream out;
    private static PrintStream err;

    public static void main(String[] args) throws IOException {
        var testClass = args[0];
        runTests(testClass);
    }

    private static void runTests(String testClass)
            throws IOException {
        // Close standard input in case some solutions read from it
        System.in.close();

        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(nullOutputStream()));
        System.setErr(new PrintStream(nullOutputStream()));

        var passed = new HashSet<String>();
        var failed = new HashSet<String>();
        var failMsgs = new TreeSet<String>();

        for (var method : findTestMethods(testClass)) {
            var startTime = currentTimeMillis();
            for (int rep = 0; rep < REPETITIONS; rep++) {
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

                if (rep > 0 && rep < REPETITIONS - 1
                        && currentTimeMillis() - startTime > TEST_TIMEOUT) {
                    // this timeout is not so bad. It just means that we are not
                    // so sure that the test is deterministic, since not all
                    // REPETITIONS were tried
                    err.println("Only " + rep + " repetitions made");
                    out.println("prop: " + INCOMPLETE_REPETITIONS);
                    break;
                }
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
                thread.join(REP_TIMEOUT); // ja ja, should calculate time left
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
