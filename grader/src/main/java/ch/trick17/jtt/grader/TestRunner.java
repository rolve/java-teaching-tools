package ch.trick17.jtt.grader;

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
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.nio.file.Path;
import java.security.*;
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
    public static final String SANDBOX_ENABLED_PROP = "ch.trick17.jtt.sandboxEnabled";

    private static final int REPETITIONS = getInteger(REPETITIONS_PROP);
    private static final int REP_TIMEOUT = getInteger(REP_TIMEOUT_PROP);
    private static final int TEST_TIMEOUT = getInteger(TEST_TIMEOUT_PROP);
    private static final boolean SANDBOX_ENABLED = getBoolean(SANDBOX_ENABLED_PROP);

    private static PrintStream out;
    private static PrintStream err;

    public static void main(String[] args) throws IOException {
        var testClass = args[0];
        var codeUnderTest = Path.of(args[1]).toUri().toURL();
        runTests(testClass, codeUnderTest);
    }

    private static void runTests(String testClass, URL codeUnderTest)
            throws IOException {
        // Close standard input in case some solutions read from it
        System.in.close();

        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(nullOutputStream()));
        System.setErr(new PrintStream(nullOutputStream()));
        currentThread().setUncaughtExceptionHandler((t, e) -> e.printStackTrace(err));

        if (SANDBOX_ENABLED) {
            Policy.setPolicy(new SandboxPolicy(codeUnderTest));
            System.setSecurityManager(new SecurityManager());
        }

        var passed = new HashSet<String>();
        var failed = new HashSet<String>();
        var failMsgs = new TreeSet<String>();

        for (var test : findTestMethods(testClass)) {
            var startTime = currentTimeMillis();
            for (int rep = 1; rep <= REPETITIONS; rep++) {
                var result = runIsolated(test);

                var name = test.getMethodName();
                if (result.isEmpty()) {
                    passed.add(name);
                } else {
                    failed.add(name);
                    var msg = name + ": "
                            + valueOf(result.get().getMessage()).replaceAll("\\s+", " ")
                            + " (" + result.get().getClass().getName() + ")";
                    // TODO: Collect exception stats
                    failMsgs.add(msg);
                    if (result.get() instanceof ThreadDeath) {
                        out.println("prop: " + TIMEOUT);
                    } else if (result.get() instanceof AccessControlException) {
                        out.println("prop: " + ILLEGAL_OPERATION);
                    }
                }

                if (rep < REPETITIONS && currentTimeMillis() - startTime > TEST_TIMEOUT) {
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
     * Runs a test in isolation, meaning that it runs with freshly loaded
     * classes. This avoids problems with static initialization. Apparently, two
     * things are required for this to work: 1) all JUnit classes must be
     * freshly loaded too, which can be achieved by referring to them from a
     * class ({@link Isolated}) loaded by a fresh class loader; and 2) the
     * context class loader, which is used by JUnit to discover test classes,
     * must be set to the same class loader.
     * <p>
     * Note that the isolation provided by this approach is not absolute:
     * classes from the Java Standard Library are shared among test runs, so
     * there may still be interference, e.g., via {@link Math#random()},
     * {@link System#setProperty(String, String)}, etc. (However, the latter
     * is prevented by default by the sandboxing mechanism.)
     */
    private static Optional<Throwable> runIsolated(MethodSource test) {
        var origLoader = currentThread().getContextClassLoader();
        var urls = classpathUrls();
        try (var loader = new URLClassLoader(urls, origLoader.getParent())) {
            currentThread().setContextClassLoader(loader);

            var cls = loader.loadClass(Isolated.class.getName());
            var method = cls.getMethod("run", String.class, String.class);

            var result = method.invoke(null, test.getClassName(), test.getMethodName());
            return Optional.ofNullable((Throwable) result);
        } catch (ReflectiveOperationException | IOException e) {
            e.printStackTrace(err);
            throw new AssertionError(e);
        } finally {
            currentThread().setContextClassLoader(origLoader);
        }
    }

    private static URL[] classpathUrls() {
        return stream(System.getProperty("java.class.path").split(pathSeparator))
                .map(path -> {
                    try {
                        return Path.of(path).toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                })
                .toArray(URL[]::new);
    }

    public static class Isolated {

        public static Throwable run(String className, String methodName) {
            var sel = selectMethod(className, methodName);
            var req = request().selectors(sel).build();

            var result = new AtomicReference<TestExecutionResult>();
            var listener = new TestExecutionListener() {
                public void executionFinished(TestIdentifier id,
                        TestExecutionResult res) {
                    if (id.isTest()) {
                        result.set(res);
                    }
                }
            };

            runKillably(() -> LauncherFactory.create().execute(req, listener));

            if (result.get() == null) { // quite unlikely but possible, I suppose
                return new ThreadDeath();
            } else {
                // no Optional here, since this method is called reflectively and
                // an unsafe cast would be needed in the calling method
                return result.get().getThrowable().orElse(null);
            }
        }

        /**
         * Runs test in a different thread so it can be killed after the timeout.
         * This is a very hard timeout ({@link Thread#stop()}) that should be able
         * to handle anything the students might do.
         */
        private static void runKillably(Runnable runnable) {
            var thread = new Thread(runnable);
            // we should be able to kill the thread (see below), but just in
            // case, if everything else fails, the thread is set to "daemon", so
            // that it is finally killed when the JVM exists:
            thread.setDaemon(true);
            thread.start();
            while (true) {
                try {
                    thread.join(REP_TIMEOUT); // ja ja, should calculate time left
                    break;
                } catch (InterruptedException e) {}
            }
            if (thread.isAlive()) {
                kill(thread);
            }
        }

        /**
         * Uses the infamous {@link Thread#stop()} method to kill the thread.
         * And, just in case someone tries to catch the thrown
         * {@link ThreadDeath}, more of them are thrown, faster and faster.
         */
        @SuppressWarnings("deprecation")
        private static void kill(Thread thread) {
            int waitTime = 100;
            do {
                thread.stop();
                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                        waitTime /= 2;
                    } catch (InterruptedException e) {}
                }
            } while (thread.isAlive());
        }
    }

    private static class SandboxPolicy extends Policy {

        private final URL codeUnderTest;

        public SandboxPolicy(URL codeUnderTest) {
            this.codeUnderTest = codeUnderTest;
        }

        public PermissionCollection getPermissions(CodeSource source) {
            var permissions = new Permissions();
            if (!codeUnderTest.equals(source.getLocation())) {
                permissions.add(new AllPermission());
            }
            return permissions;
        }
    }
}
