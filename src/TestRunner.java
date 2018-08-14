import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.ComparisonFailure;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;

public class TestRunner {
    
    static final Set<Class<? extends Throwable>> junitExceptions = new HashSet<>(asList(
            ComparisonFailure.class, ArrayComparisonFailure.class,
            AssertionError.class, TestTimedOutException.class));
    
    static final Set<Class<? extends Throwable>> knownExceptions = new HashSet<>(asList(
            StackOverflowError.class, OutOfMemoryError.class, NullPointerException.class,
            ArrayIndexOutOfBoundsException.class, StringIndexOutOfBoundsException.class,
            IndexOutOfBoundsException.class, InputMismatchException.class,
            NoSuchElementException.class, FileNotFoundException.class,
            IllegalArgumentException.class));
    
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        String testClass = args[0];
        Set<String> classes = stream(args).skip(1).collect(toSet());
        
        new TestRunner(testClass, classes).runTests();
    }

    private String testClass;
    private Set<String> classes;
    
    public TestRunner(String testClass, Set<String> classes) {
        this.testClass = testClass;
        this.classes = classes;
    }

    private void runTests() throws IOException, ClassNotFoundException {
        Set<String> all = new HashSet<>();
        Set<String> failed = new HashSet<>();

        // Close standard input in case some solutions read from it
        System.in.close();

        PrintStream stdOut = System.out;
        PrintStream stdErr = System.err;
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));

        JUnitCore core = new JUnitCore();
        core.addListener(new RunListener() {
            public void testFinished(Description description) {
                all.add(description.getMethodName());
            }
            public void testFailure(Failure failure) throws Exception {
                failed.add(failure.getDescription().getMethodName());
                
                String msg = failure.toString();
                Throwable exception = failure.getException();
                if (dontPrintTrace(exception)) {
                    stdErr.println(msg + " (" + exception.getClass().getName() + ")");
                } else {
                    stdErr.println(msg);
                    exception.printStackTrace(stdErr);
                }
            }
        });
        core.run(Class.forName(testClass));

        all.removeAll(failed);
        all.stream().forEach(stdOut::println);
        stdOut.flush();
        stdErr.flush();
    }
    
    private boolean dontPrintTrace(Throwable exception) {
        Class<? extends Throwable> clazz = exception.getClass();
        
        boolean inCodeUnderTest = stream(exception.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(classes::contains);
        
        return inCodeUnderTest && knownExceptions.contains(clazz) ||
                junitExceptions.contains(clazz);
    }
}
