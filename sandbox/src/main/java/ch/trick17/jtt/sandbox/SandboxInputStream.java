package ch.trick17.jtt.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SandboxInputStream extends InputStream {

    private final ThreadLocal<InputStream> sandboxed = new InheritableThreadLocal<>();

    final InputStream unsandboxed;

    public SandboxInputStream(InputStream unsandboxed) {
        this.unsandboxed = unsandboxed;
    }

    public void activate(InputStream sandboxedStream) {
        sandboxed.set(sandboxedStream);
    }

    public void deactivate() {
        sandboxed.remove();
    }

    private InputStream delegate() {
        var sandboxed = this.sandboxed.get();
        return sandboxed != null ? sandboxed : unsandboxed;
    }

    /* Delegate methods */

    @Override
    public int read() throws IOException {
        return delegate().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate().read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return delegate().readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return delegate().readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return delegate().readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate().skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate().available();
    }

    @Override
    public void close() throws IOException {
        delegate().close();
    }

    @Override
    public void mark(int readlimit) {
        delegate().mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate().reset();
    }

    @Override
    public boolean markSupported() {
        return delegate().markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return delegate().transferTo(out);
    }
}
