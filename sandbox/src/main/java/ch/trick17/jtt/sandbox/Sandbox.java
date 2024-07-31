package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.memcompile.ClassPath;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static ch.trick17.jtt.sandbox.InputMode.CLOSED;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.*;
import static ch.trick17.jtt.sandbox.SandboxClassLoader.RE_INIT_METHOD;
import static java.io.InputStream.nullInputStream;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.ClassLoader.getPlatformClassLoader;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A sandbox for running code in isolation. Sandboxed code is (re)loaded using a
 * separate class loader, so its changes to static state are invisible to the
 * caller. In addition, the code can be run with restricted permissions, with a
 * timeout, and/or with custom standard input/output handling.
 */
public class Sandbox implements Closeable {

    private static volatile SandboxInputStream stdIn;
    private static volatile SandboxPrintStream stdOut;
    private static volatile SandboxPrintStream stdErr;

    private final SandboxClassLoader loader;
    private final Duration timeout;
    private final InputMode stdInMode;
    private final OutputMode stdOutMode;
    private final OutputMode stdErrMode;

    /**
     * Creates a new sandbox with the given class paths for the sandboxed
     * code and the support code. The sandboxed code is loaded in a new
     * class loader, together with the support code, but only the sandboxed
     * code is instrumented to restrict its permissions and to react to
     * timeouts.
     * <p>
     * For more options, use {@link Builder}.
     */
    public Sandbox(ClassPath sandboxedCode, ClassPath supportCode) throws IOException {
        this(new Builder(sandboxedCode, supportCode));
    }

    private Sandbox(Builder builder) throws IOException {
        loader = new SandboxClassLoader(builder.sandboxedCode,
                builder.supportCode, builder.permittedCalls,
                builder.timeout != null, getPlatformClassLoader());
        this.timeout = builder.timeout;
        this.stdInMode = builder.stdInMode;
        this.stdOutMode = builder.stdOutMode;
        this.stdErrMode = builder.stdErrMode;
    }

    /**
     * Runs the specified static (!) method with the given parameters in the
     * sandbox. The return value of the method is returned, but be aware that it
     * could be an object of a class loaded by a different class loader, making
     * it unusable without reflection. (This is not the case for classes loaded
     * by the bootstrap class loader, like String).
     */
    public <T> Result<T> run(Class<?> cls, String methodName,
                             List<Class<?>> paramTypes, List<?> args,
                             Class<T> resultType) {
        return run(cls.getName(), methodName, paramTypes, args, resultType);
    }

    /**
     * Runs the specified static (!) method with the given parameters in the
     * sandbox. The return value of the method is returned, but be aware that it
     * could be an object of a class loaded by a different class loader, making
     * it unusable without reflection. (This is not the case for classes loaded
     * by the bootstrap class loader, like String).
     */
    public <T> Result<T> run(String className, String methodName,
                             List<Class<?>> paramTypes, List<?> args,
                             Class<T> resultType) {
        // Re-initialize sandboxed classes, in the same order they were
        // originally loaded. The first time the sandbox is used, no classes
        // have been loaded yet, so this loop terminates immediately.
        for (var c : loader.getSandboxedClasses()) {
            try {
                var reInit = c.getMethod(RE_INIT_METHOD);
                reInit.setAccessible(true);
                reInit.invoke(null);
            } catch (NoSuchMethodException ignored) {
                // only classes with static state have this method
            } catch (NoClassDefFoundError ignored) {
                // ignore; if this happens, this class cannot be used anyway,
                // so isolation should not be affected. May happen if an
                // exception was thrown in the static initializer of the class.
            } catch (IllegalAccessException | InvocationTargetException e) {
                // NoClassDefFoundError may be wrapped in an InvocationTargetException
                if (!(e.getCause() instanceof NoClassDefFoundError)) {
                    throw new AssertionError("Could not re-initialize class " + c, e);
                }
            }
        }

        Action<T> isolated = () -> {
            var cls = loader.loadClass(className);
            var method = cls.getMethod(methodName, paramTypes.toArray(Class<?>[]::new));
            var runner = new CustomCxtClassLoaderRunner(loader);
            try {
                return runner.run(() -> resultType.cast(method.invoke(null, args.toArray())));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };

        Action<T> timed = timeout != null ? () -> runWithTimeout(isolated) : isolated;

        Supplier<Result<T>> asResult = () -> {
            try {
                var value = timed.run();
                return Result.normal(value);
            } catch (TimeoutException e) {
                return Result.timeout();
            } catch (OutOfMemoryError e) {
                System.gc(); // may or may not help...
                return Result.outOfMemory(e);
            } catch (SecurityException e) {
                return Result.illegalOperation(e);
            } catch (Throwable e) {
                return Result.exception(e);
            }
        };

        if (stdInMode != InputMode.NORMAL || stdOutMode != NORMAL || stdErrMode != NORMAL) {
            ensureStreamsInstalled();
            if (stdInMode == EMPTY || stdInMode == CLOSED) {
                stdIn.activate(nullInputStream());
                if (stdInMode == InputMode.CLOSED) {
                    try {
                        stdIn.close();
                    } catch (IOException ignored) {}
                }
            }
            var outRecorder = activatePrintStream(stdOut, stdOutMode);
            var errRecorder = activatePrintStream(stdErr, stdErrMode);
            try {
                var result = asResult.get();
                if (outRecorder != null) {
                    result.setStdOut(outRecorder.toString(UTF_8));
                }
                if (errRecorder != null) {
                    result.setStdErr(errRecorder.toString(UTF_8));
                }
                return result;
            } finally {
                stdIn.deactivate();
                stdOut.deactivate();
                stdErr.deactivate();
            }
        } else {
            return asResult.get();
        }
    }

    private ByteArrayOutputStream activatePrintStream(SandboxPrintStream stream,
                                                      OutputMode mode) {
        ByteArrayOutputStream recorder = null;
        OutputStream sandboxed;
        if (mode == NORMAL) {
            return null; // don't activate
        } else if (mode == RECORD) {
            sandboxed = recorder = new ByteArrayOutputStream();
        } else if (mode == RECORD_FORWARD) {
            recorder = new ByteArrayOutputStream();
            sandboxed = new TeeOutputStream(recorder, stream.unsandboxed);
        } else { // DISCARD
            sandboxed = nullOutputStream();
        }
        stream.activate(sandboxed);
        return recorder;
    }

    /**
     * Runs the code in a different thread, so it can be "killed" after the
     * timeout. Killing the thread works by simply interrupting it. The bytecode
     * instrumentation performed by the SandboxClassLoader ensures that the
     * sandboxed code actually reacts to the interruption.
     */
    private <T> T runWithTimeout(Action<T> action) throws Throwable {
        var task = new FutureTask<>(action.asCallable());
        var thread = new Thread(task);
        // we should be able to kill the thread, but just in case, the thread is
        // set to "daemon", so that it does not prevent the JVM from exiting.
        thread.setDaemon(true);
        thread.start();

        while (true) {
            try {
                try {
                    return task.get(MILLISECONDS.convert(timeout), MILLISECONDS);
                } catch (ExecutionException e) {
                    throw e.getCause().getCause(); // asCallable wraps again...
                } catch (TimeoutException e) {
                    while (thread.isAlive()) {
                        thread.interrupt();
                        MILLISECONDS.sleep(50);
                    }
                    throw e;
                }
            } catch (InterruptedException ignored) {}
        }
    }

    private static void ensureStreamsInstalled() {
        if (stdIn == null) {
            synchronized (Sandbox.class) {
                if (stdIn == null) {
                    var in = new SandboxInputStream(System.in);
                    stdOut = new SandboxPrintStream(System.out);
                    stdErr = new SandboxPrintStream(System.err);
                    System.setIn(in);
                    System.setOut(stdOut);
                    System.setErr(stdErr);

                    stdIn = in; // signal for other threads that everything is ready
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        loader.close();
    }

    public static class Builder {
        private final ClassPath sandboxedCode;
        private final ClassPath supportCode;

        private Whitelist permittedCalls = Whitelist.getDefault();
        private Duration timeout = null;
        private InputMode stdInMode = InputMode.NORMAL;
        private OutputMode stdOutMode = NORMAL;
        private OutputMode stdErrMode = NORMAL;

        /**
         * Builds a new sandbox with the given class paths for the sandboxed
         * code and the support code. The sandboxed code is loaded in a new
         * class loader, together with the support code, but only the sandboxed
         * code is instrumented to restrict its permissions and to react to
         * timeouts.
         */
        public Builder(ClassPath sandboxedCode, ClassPath supportCode) {
            this.sandboxedCode = sandboxedCode;
            this.supportCode = supportCode;
        }

        /**
         * Sets the list of permitted method/constructor calls for the sandbox.
         * If not set, the whitelist returned by {@link Whitelist#getDefault()}
         * is used. If set to <code>null</code>, no restrictions are applied.
         */
        public Builder permittedCalls(Whitelist permittedCalls) {
            this.permittedCalls = permittedCalls;
            return this;
        }

        /**
         * Sets a timeout for the code to be executed. If a timeout is set, the
         * code is executed in a different thread that is forcefully terminated
         * when the timeout is over. By default, the timeout is set to
         * <code>null</code>, meaning it is disabled.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder stdInMode(InputMode stdInMode) {
            this.stdInMode = stdInMode;
            return this;
        }

        /**
         * Determines how to handle output to <code>System.out</code>. The
         * default mode is {@link OutputMode#NORMAL}. Note that the sandboxed
         * code may affect the I/O behavior using
         * {@link System#setOut(PrintStream)}, unless it runs with restricted
         * permissions.
         */
        public Builder stdOutMode(OutputMode stdOutMode) {
            this.stdOutMode = requireNonNull(stdOutMode);
            return this;
        }

        /**
         * Determines how to handle output to <code>System.err</code>. The
         * default mode is {@link OutputMode#NORMAL}. Note that the sandboxed
         * code may affect the I/O behavior using
         * {@link System#setErr(PrintStream)}, unless it runs with restricted
         * permissions.
         */
        public Builder stdErrMode(OutputMode stdErrMode) {
            this.stdErrMode = requireNonNull(stdErrMode);
            return this;
        }

        public Sandbox build() throws IOException {
            return new Sandbox(this);
        }
    }

    /**
     * Like Callable, but allows {@link Throwable} to be thrown, not just
     * {@link Exception}.
     */
    interface Action<T> {
        T run() throws Throwable;

        default Callable<T> asCallable() { // needed for FutureTask
            return () -> {
                try {
                    return run();
                } catch (Throwable e) {
                    throw new Exception(e);
                }
            };
        }
    }

    public static class Result<T> {

        public static <T> Result<T> normal(T value) {
            return new Result<>(Kind.NORMAL, value, null);
        }

        public static <T> Result<T> exception(Throwable exception) {
            return new Result<>(Kind.EXCEPTION, null, exception);
        }

        public static <T> Result<T> timeout() {
            return new Result<>(Kind.TIMEOUT, null, null);
        }

        public static <T> Result<T> outOfMemory(OutOfMemoryError error) {
            return new Result<>(Kind.OUT_OF_MEMORY, null, error);
        }

        public static <T> Result<T> illegalOperation(SecurityException exception) {
            return new Result<>(Kind.ILLEGAL_OPERATION, null, exception);
        }

        private final Kind kind;
        private final T value;
        private final Throwable exception;
        private String stdOut = null;
        private String stdErr = null;

        private Result(Kind kind, T value, Throwable exception) {
            this.kind = kind;
            this.value = value;
            this.exception = exception;
        }

        public Kind kind() {
            return kind;
        }

        public T value() {
            if (kind != Kind.NORMAL) {
                throw new IllegalStateException("no value", exception);
            }
            return value;
        }

        public Throwable exception() {
            if (exception == null) {
                throw new IllegalStateException();
            }
            return exception;
        }

        /**
         * The standard output that was recorded. If recording was not enabled,
         * returns <code>null</code>.
         */
        public String stdOut() {
            return stdOut;
        }

        /**
         * The standard error output that was recorded. If recording was not
         * enabled, returns <code>null</code>.
         */
        public String stdErr() {
            return stdErr;
        }

        void setStdOut(String stdOut) {
            this.stdOut = stdOut;
        }

        void setStdErr(String stdErr) {
            this.stdErr = stdErr;
        }

        public enum Kind {
            NORMAL, EXCEPTION, TIMEOUT, OUT_OF_MEMORY, ILLEGAL_OPERATION;
        }
    }
}
