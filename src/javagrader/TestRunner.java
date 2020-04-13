package javagrader;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;

import org.junit.ComparisonFailure;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.runners.model.TestTimedOutException;
import org.opentest4j.AssertionFailedError;

public class TestRunner {

    static final Set<Class<? extends Throwable>> junitExceptions = Set.of(
            ComparisonFailure.class, ArrayComparisonFailure.class,
            AssertionError.class, TestTimedOutException.class,
            AssertionFailedError.class);

    static final Set<Class<? extends Throwable>> knownExceptions = Set.of(
            StackOverflowError.class, OutOfMemoryError.class,
            NullPointerException.class, ArrayIndexOutOfBoundsException.class,
            StringIndexOutOfBoundsException.class,
            IndexOutOfBoundsException.class, InputMismatchException.class,
            NoSuchElementException.class, FileNotFoundException.class,
            IllegalArgumentException.class, NumberFormatException.class,
            ArithmeticException.class, EmptyStackException.class,
            PatternSyntaxException.class, IllegalStateException.class);

    private static final int REPETITIONS = 7;

    // For how long we do REPETITIONS. If we were running tests for more than
    // this time, do not attempt another repetition.
    private static final long MAX_RUNNING_TIME = 10000; // ms

    public static PrintStream out;
    public static PrintStream err;

    private static int repetition;

    public static void main(String[] args) throws Exception {
        var testClass = args[0];
        var classes = stream(args).skip(1).collect(toSet());
        runTests(testClass, classes);
    }

    private static void runTests(String testClass, Set<String> classes)
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
        var failedTests = new HashSet<String>();
        var failMsgs = new TreeSet<String>();

        var startTime = currentTimeMillis();
        for (repetition = 0; repetition < REPETITIONS; repetition++) {
            var passed = new HashSet<String>();
            runTestsOnce(testClass, new TestExecutionListener() {
                public void executionFinished(TestIdentifier id,
                        TestExecutionResult res) {
                    if (!id.isTest()) {
                        return;
                    }
                    var name = id.getDisplayName();
                    if (res.getStatus() == SUCCESSFUL) {
                        passed.add(name);
                    } else {
                        failedTests.add(name);
                        var exception = res.getThrowable().get();
                        var msg = name + ": " + exception.getMessage()
                                + " (" + exception.getClass().getName() + ")";
                        if (printTrace(exception, classes)) {
                            msg += "\n" + stream(exception.getStackTrace())
                                    .map(e -> "        at " + e)
                                    .collect(joining("\n"));
                        }
                        failMsgs.add(msg);
                    }
                }
            });
            passedSets.add(passed);

            if (repetition > 0 && repetition < REPETITIONS - 1
                    && currentTimeMillis() - startTime > MAX_RUNNING_TIME) {
                // this timeout is not so bad. It just means that we are not so
                // sure that the tests are deterministic, since not all
                // REPETITIONS were tried
                out.println("SOFT_TIMEOUT");
                err.println("SOFT_TIMEOUT");
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
            var nonDeterm = new HashSet<>(failedTests);
            var allPassed = new HashSet<String>(); 
            for (var passed : passedSets) {
                allPassed.addAll(passed);
            }
            nonDeterm.retainAll(allPassed);

            err.println("Non-determinism detected in tests: " + nonDeterm);
            out.println("nondeterministic");
        }

        var alwaysSucc = passedSets.iterator().next();
        alwaysSucc.removeAll(failedTests);

        alwaysSucc.forEach(out::println);
        out.flush();
        err.flush();
    }

    public static void runTestsOnce(String testClass, TestExecutionListener listener)
            throws Exception {
        var launcher = LauncherFactory.create();
        var request = request().selectors(selectClass(testClass)).build();
        launcher.execute(request, listener);
    }

    private static boolean printTrace(Throwable e, Set<String> classes) {
        var clazz = e.getClass();

        var inCodeUnderTest = stream(e.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(classes::contains);

        var ignore = inCodeUnderTest && knownExceptions.contains(clazz)
                || junitExceptions.contains(clazz);
        return !ignore;
    }
}
