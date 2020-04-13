package javagrader;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;

import org.junit.ComparisonFailure;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;
import org.opentest4j.AssertionFailedError;

public class TestRunner {

    static final Set<Class<? extends Throwable>> junitExceptions = Set.of(
            ComparisonFailure.class, ArrayComparisonFailure.class,
            AssertionError.class, TestTimedOutException.class,
            AssertionFailedError.class);

    static final Set<Class<? extends Throwable>> knownExceptions = Set.of(
            StackOverflowError.class, OutOfMemoryError.class, NullPointerException.class,
            ArrayIndexOutOfBoundsException.class, StringIndexOutOfBoundsException.class,
            IndexOutOfBoundsException.class, InputMismatchException.class,
            NoSuchElementException.class, FileNotFoundException.class,
            IllegalArgumentException.class, NumberFormatException.class,
            ArithmeticException.class, EmptyStackException.class, 
            PatternSyntaxException.class, IllegalStateException.class);

    private static final int REPETITIONS = 7;

    // For how long we do REPETITIONS. If we were running tests for more than this time, do not
    // attempt another repetition.
    private static final long MAX_RUNNING_TIME = 10000; // millisecs

    public static PrintStream out;
    public static PrintStream err;

    private static int repetition;

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        String testClass = args[0];
        Set<String> classes = stream(args).skip(1).collect(toSet());
        runTests(testClass, classes);
    }

    private static void runTests(String testClass, Set<String> classes) throws IOException, ClassNotFoundException {
        // Close standard input in case some solutions read from it
        System.in.close();
        
        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));

        Set<String> failedTests = new HashSet<>();
        SortedSet<String> failures = new TreeSet<>();
        List<Set<String>> passedTests = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        for (repetition = 0; repetition < REPETITIONS; repetition++) {
            Set<String> all = new HashSet<>();
            Set<String> failed = new HashSet<>();
            JUnitCore core = new JUnitCore();
            core.addListener(new RunListener() {
                public void testFinished(Description description) {
                    all.add(description.getMethodName());
                }
                public void testFailure(Failure failure) throws Exception {
                    String name = failure.getDescription().getMethodName();
                    failed.add(name);
                    failedTests.add(name);

                    String msg = failure.toString();
                    Throwable exception = failure.getException();
                    if (dontPrintTrace(exception, classes)) {
                        msg += " (" + exception.getClass().getName() + ")";
                    } else {
                        msg += "\n" + failure.getTrace();
                    }
                    failures.add(msg);
                }
            });
            core.run(Class.forName(testClass));
            all.removeAll(failed);
            passedTests.add(all);

            if (repetition > 0 && repetition < REPETITIONS - 1 && System.currentTimeMillis() - startTime > MAX_RUNNING_TIME) {
                // this timeout is not so bad. It just means that we are not pretty sure that the
                // tests are deterministic, since not all REPETITIONS were tried
                out.println("SOFT_TIMEOUT");
                err.println("SOFT_TIMEOUT");
                break;
            }
        }

        failures.stream()
                .map(s -> stream(s.split("\n")).map(l -> "    " + l).collect(joining("\n")))
                .forEach(err::println);

        List<Set<String>> distinctPassed = passedTests.stream().distinct().collect(toList());
        if (distinctPassed.size() > 1) {
            // take the intersection of all tests that once failed tests with all tests that once
            // passed tests.
            Set<String> nonDeterm = new HashSet<>(failedTests);
            Set<String> allPassed = new HashSet<>(); 
            for (Set<String> passed : distinctPassed) {
                allPassed.addAll(passed);
            }
            nonDeterm.retainAll(allPassed);
            
            err.println("Non-determinism detected in tests: " + nonDeterm);
            out.println("nondeterministic");
        }

        Set<String> alwaysSucc = new HashSet<>(distinctPassed.get(0));
        alwaysSucc.removeAll(failedTests);

        alwaysSucc.forEach(out::println);
        out.flush();
        err.flush();
    }

    private static boolean dontPrintTrace(Throwable exception, Set<String> classes) {
        Class<? extends Throwable> clazz = exception.getClass();

        boolean inCodeUnderTest = stream(exception.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(classes::contains);

        return inCodeUnderTest && knownExceptions.contains(clazz) ||
                junitExceptions.contains(clazz);
    }

    public static void staticCheck(Consumer<PrintStream> code) {
        if (repetition == 0) {
            code.accept(err);
        }
    }
}
