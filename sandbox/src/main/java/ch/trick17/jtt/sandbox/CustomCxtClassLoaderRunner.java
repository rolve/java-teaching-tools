package ch.trick17.jtt.sandbox;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static java.lang.Thread.currentThread;

public class CustomCxtClassLoaderRunner {

    private final ClassLoader loader;

    public CustomCxtClassLoaderRunner(ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    public <T> T call(Callable<T> action) throws Exception {
        var origLoader = currentThread().getContextClassLoader();
        var closeable = loader instanceof AutoCloseable ? (AutoCloseable) loader : null;
        try (closeable) {
            currentThread().setContextClassLoader(loader);
            return action.call();
        } finally {
            currentThread().setContextClassLoader(origLoader);
        }
    }

    public <T> T run(Supplier<T> action) {
        try {
            return call(action::get);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void run(Runnable action) {
        run(() -> {
            action.run();
            return null;
        });
    }
}
