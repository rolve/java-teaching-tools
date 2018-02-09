import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.ComparisonFailure;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;

public class TestRunner {
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        String testClass = args[0];
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
                stdErr.println(failure);

                Throwable exception = failure.getException();
                if(!(exception instanceof ComparisonFailure ||
                        exception instanceof AssertionError ||
                        exception instanceof TestTimedOutException)) {
                    exception.setStackTrace(Stream.of(exception.getStackTrace())
                            .distinct().toArray(StackTraceElement[]::new));
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
}
