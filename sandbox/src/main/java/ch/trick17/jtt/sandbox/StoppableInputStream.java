package ch.trick17.jtt.sandbox;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.max;

/**
 * Wraps an {@link InputStream} with a possibly uninterruptible and, more
 * importantly for the sandbox, unstoppable <code>read</code> method (such
 * as {@link java.io.FileInputStream}, including <code>System.in</code>),
 * so that it can be {@linkplain Thread#stop() stopped}, e.g., after a timeout.
 * <p>
 * Note that this class should only be used for infinite streams such as
 * <code>System.in</code>, as the {@link #read()} implementation simply
 * spin-waits until {@link #available()} returns > 0, and thus cannot
 * distinguish whether the wrapped stream is empty or just waiting for more
 * input.
 */
public class StoppableInputStream extends InputStream {

    // TODO: use this class in the sandbox when InputMode is NORMAL

    private final InputStream wrapped;

    public StoppableInputStream(InputStream wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        while (wrapped.available() < 1) {
            Thread.onSpinWait();
        }
        return wrapped.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        // read only as many bytes as are available, but at least 1
        return super.read(b, off, max(1, available()));
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
