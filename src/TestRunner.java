import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.ComparisonFailure;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;

public class TestRunner {
    public static void main(String[] args) throws ClassNotFoundException {
        String testClass = args[0];
        Set<String> all = new HashSet<>();
        Set<String> failed = new HashSet<>();

        PrintStream stdOut = System.out;
        System.setOut(System.err);

        JUnitCore core = new JUnitCore();
        core.addListener(new RunListener() {
            public void testFinished(Description description) {
                all.add(description.getMethodName());
            }
            public void testFailure(Failure failure) throws Exception {
                failed.add(failure.getDescription().getMethodName());
                System.err.println(failure);

                Throwable exception = failure.getException();
                if(!(exception instanceof ComparisonFailure ||
                        exception instanceof AssertionError ||
                        exception instanceof TestTimedOutException)) {
                    exception.printStackTrace();
                }
            }
        });
        core.run(Class.forName(testClass));

        all.removeAll(failed);
        all.stream().forEach(stdOut::println);
    }
}
