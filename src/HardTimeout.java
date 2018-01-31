import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

public class HardTimeout implements TestRule {

    private static final List<String> filterClasses = asList(
            Thread.class.getName(),
            FutureTask.class.getName(),
            HardTimeout.class.getName());

    private long timeout;
    private TimeUnit unit;

    public HardTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @SuppressWarnings("deprecation")
            public void evaluate() throws Throwable {
                AtomicBoolean started = new AtomicBoolean();

                FutureTask<Throwable> task = new FutureTask<Throwable>(() -> {
                    try {
                        started.set(true);;
                        base.evaluate();
                        return null;
                    } catch(Throwable t) {
                        return t;
                    }
                });

                Thread thread = new Thread(task, "Time-limited test");
                thread.setDaemon(true);
                thread.start();

                while (!started.get()) {}

                while (true) {
                    try {
                        Throwable throwable = task.get(timeout, unit);
                        if (throwable != null) {
                            throwable.setStackTrace(cleanUp(throwable.getStackTrace()));
                            throw throwable;
                        }
                        break;
                    } catch(TimeoutException e) {
                        StackTraceElement[] stackTrace = cleanUp(thread.getStackTrace());

                        thread.stop(); // <- this

                        TestTimedOutException timedOut = new TestTimedOutException(timeout, unit);
                        timedOut.setStackTrace(stackTrace);
                        throw timedOut;
                    } catch(InterruptedException e) {
                        // try again
                    }
                }
            }
        };
    }

    private StackTraceElement[] cleanUp(StackTraceElement[] stackTrace) {
        return Stream.of(stackTrace)
                .filter(e -> !filterClasses.stream().anyMatch(name -> e.getClassName().startsWith(name)))
                .toArray(StackTraceElement[]::new);
    }
}
