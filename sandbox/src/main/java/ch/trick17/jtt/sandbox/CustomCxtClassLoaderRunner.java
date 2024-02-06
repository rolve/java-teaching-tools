package ch.trick17.jtt.sandbox;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import static java.lang.Thread.currentThread;

public class CustomCxtClassLoaderRunner implements Closeable {

    private final ClassLoader loader;

    public CustomCxtClassLoaderRunner(ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    public <T, E extends Throwable> T run(Action<T, E> action) throws E {
        var origLoader = currentThread().getContextClassLoader();
        try {
            currentThread().setContextClassLoader(loader);
            return action.run();
        } finally {
            currentThread().setContextClassLoader(origLoader);
        }
    }

    public interface Action<T, E extends Throwable> {
        T run() throws E;
    }

    @Override
    public void close() throws IOException {
        if (loader instanceof Closeable c) {
            c.close();
        }
    }
}
