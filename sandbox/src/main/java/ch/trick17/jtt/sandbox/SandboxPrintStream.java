package ch.trick17.jtt.sandbox;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

import static ch.trick17.jtt.sandbox.Sandbox.SANDBOX;

class SandboxPrintStream extends PrintStream {

    private final ThreadLocal<PrintStream> sandboxed = new InheritableThreadLocal<>();

    final PrintStream unsandboxed;

    public SandboxPrintStream(PrintStream unsandboxed) {
        super(nullOutputStream()); // delegate everything
        this.unsandboxed = unsandboxed;
    }

    public void activate(OutputStream sandboxedStream) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SANDBOX);
        }
        sandboxed.set(new PrintStream(sandboxedStream));
    }

    public void deactivate() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SANDBOX);
        }
        sandboxed.set(null);
    }

    private PrintStream delegate() {
        var sandboxed = this.sandboxed.get();
        return sandboxed != null ? sandboxed : unsandboxed;
    }

    /* Delegate methods */

    @Override
    public void flush() {
        delegate().flush();
    }

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public boolean checkError() {
        return delegate().checkError();
    }

    @Override
    public void write(int b) {
        delegate().write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        delegate().write(buf, off, len);
    }

    @Override
    public void print(boolean b) {
        delegate().print(b);
    }

    @Override
    public void print(char c) {
        delegate().print(c);
    }

    @Override
    public void print(int i) {
        delegate().print(i);
    }

    @Override
    public void print(long l) {
        delegate().print(l);
    }

    @Override
    public void print(float f) {
        delegate().print(f);
    }

    @Override
    public void print(double d) {
        delegate().print(d);
    }

    @Override
    public void print(char[] s) {
        delegate().print(s);
    }

    @Override
    public void print(String s) {
        delegate().print(s);
    }

    @Override
    public void print(Object obj) {
        delegate().print(obj);
    }

    @Override
    public void println() {
        delegate().println();
    }

    @Override
    public void println(boolean x) {
        delegate().println(x);
    }

    @Override
    public void println(char x) {
        delegate().println(x);
    }

    @Override
    public void println(int x) {
        delegate().println(x);
    }

    @Override
    public void println(long x) {
        delegate().println(x);
    }

    @Override
    public void println(float x) {
        delegate().println(x);
    }

    @Override
    public void println(double x) {
        delegate().println(x);
    }

    @Override
    public void println(char[] x) {
        delegate().println(x);
    }

    @Override
    public void println(String x) {
        delegate().println(x);
    }

    @Override
    public void println(Object x) {
        delegate().println(x);
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        return delegate().printf(format, args);
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        return delegate().printf(l, format, args);
    }

    @Override
    public PrintStream format(String format, Object... args) {
        return delegate().format(format, args);
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        return delegate().format(l, format, args);
    }

    @Override
    public PrintStream append(CharSequence csq) {
        return delegate().append(csq);
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        return delegate().append(csq, start, end);
    }

    @Override
    public PrintStream append(char c) {
        return delegate().append(c);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate().write(b);
    }
}
