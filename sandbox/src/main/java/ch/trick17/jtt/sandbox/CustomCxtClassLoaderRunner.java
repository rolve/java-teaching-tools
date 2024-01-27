package ch.trick17.jtt.sandbox;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static java.lang.Thread.currentThread;

public class CustomCxtClassLoaderRunner implements Closeable {

    private final ClassLoader loader;

    public CustomCxtClassLoaderRunner(ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    public <T> T call(Callable<T> action) throws Exception {
        var origLoader = currentThread().getContextClassLoader();
        try {
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

    @Override
    public void close() throws IOException {
        if (loader instanceof Closeable c) {
            c.close();
        }
    }
}
