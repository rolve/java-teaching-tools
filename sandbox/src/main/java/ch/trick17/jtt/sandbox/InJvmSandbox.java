package ch.trick17.jtt.sandbox;

import org.apache.commons.io.output.TeeOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.security.Policy;
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
import static java.io.InputStream.nullInputStream;
import static java.io.OutputStream.nullOutputStream;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Stream.concat;

public class InJvmSandbox {

    // could be anything at the moment, as the policy is all-or-nothing anyway
    static final Permission SANDBOX = new RuntimePermission("sandbox");

    private static volatile SandboxPolicy policy;
    private static volatile SandboxInputStream stdIn;
    private static volatile SandboxPrintStream stdOut;
    private static volatile SandboxPrintStream stdErr;

    private boolean permRestrictions = true;
    private boolean staticStateIsolation = true;
    private Duration timeout = null;
    private InputMode stdInMode = InputMode.NORMAL;
    private OutputMode stdOutMode = NORMAL;
    private OutputMode stdErrMode = NORMAL;

    /**
     * Enables or disables permission restrictions. If enabled, a
     * {@link SecurityManager} will restrict the permissions for the
     * code specified as 'restricted' when the
     * {@link #run(List, List, String, String, List, List)} method
     * is called. By default, restrictions are enabled.
     */
    public InJvmSandbox permRestrictions(boolean permRestrictions) {
        this.permRestrictions = permRestrictions;
        return this;
    }

    /**
     * Enables or disables isolated execution. If enabled, the code is
     * executed with freshly loaded classes, so that no interference
     * via static fields is possible. This does not apply to system
     * classes loaded by a parent class loader (such as
     * {@link java.util.Random}). By default, this isolation is enabled.
     */
    public InJvmSandbox staticStateIsolation(boolean staticStateIsolation) {
        this.staticStateIsolation = staticStateIsolation;
        return this;
    }

    /**
     * Sets a timeout for the code to be executed. If a timeout is set,
     * the code is executed in a different thread that is forcefully
     * terminated when the timeout is over. By default, the timeout is
     * set to <code>null</code>, meaning it is disabled.
     */
    public InJvmSandbox timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public InJvmSandbox stdInMode(InputMode stdInMode) {
        this.stdInMode = stdInMode;
        return this;
    }

    /**
     * Determines how to handle output to <code>System.out</code>. The
     * default mode is {@link OutputMode#NORMAL}. Note that the sandboxed
     * code may affect the I/O behavior using {@link System#setOut(PrintStream)},
     * unless it runs with restricted permissions.
     */
    public InJvmSandbox stdOutMode(OutputMode stdOutMode) {
        this.stdOutMode = requireNonNull(stdOutMode);
        return this;
    }

    /**
     * Determines how to handle output to <code>System.err</code>. The
     * default mode is {@link OutputMode#NORMAL}. Note that the sandboxed
     * code may affect the I/O behavior using {@link System#setErr(PrintStream)},
     * unless it runs with restricted permissions.
     */
    public InJvmSandbox stdErrMode(OutputMode stdErrMode) {
        this.stdErrMode = requireNonNull(stdErrMode);
        return this;
    }

    /**
     * Runs the specified static (!) method with the given parameters
     * in the sandbox. The return value of the method is returned, but
     * be aware that it could be an object of a class loaded by a
     * different class loader (if static state isolation is enabled),
     * making it unusable without reflection. (This is not the case for
     * classes loaded by the bootstrap class loader, like String).
     */
    public <T> SandboxResult<T> run(List<URL> restrictedCode, List<URL> unrestrictedCode,
                     String className, String methodName,
                     List<Class<?>> paramTypes, List<?> args) {
        Callable<T> action = () -> {
            var cls = currentThread().getContextClassLoader().loadClass(className);
            var method = cls.getMethod(methodName, paramTypes.toArray(Class<?>[]::new));
            try {
                @SuppressWarnings("unchecked")
                var result = (T) method.invoke(null, args.toArray());
                return result;
            } catch (InvocationTargetException e) {
                throw asException(e.getTargetException());
            }
        };

        Callable<T> timed = timeout != null ? () -> runWithTimeout(action) : action;

        Callable<T> isolated = staticStateIsolation ? () -> {
            var allCode = concat(restrictedCode.stream(), unrestrictedCode.stream())
                    .toArray(URL[]::new);
            var parent = currentThread().getContextClassLoader().getParent();
            var loader = new URLClassLoader(allCode, parent);
            return new CustomCxtClassLoaderRunner(loader).call(timed);
        } : timed;

        Callable<T> restricted = permRestrictions ? () -> {
            ensureSecurityInstalled();
            policy.activate(unrestrictedCode);
            try {
                return isolated.call();
            } finally {
                policy.deactivate();
            }
        } : isolated;

        Supplier<SandboxResult<T>> asResult = () -> {
            try {
                return SandboxResult.normal(restricted.call());
            } catch (TimeoutException e) {
                return SandboxResult.timeout();
            } catch (SecurityException e) {
                return SandboxResult.illegalOperation(e);
            } catch (Throwable e) {
                return SandboxResult.exception(e);
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

    public <T> SandboxResult<T> run(List<URL> restrictedCode, List<URL> unrestrictedCode,
                                    Class<?> cls, String methodName,
                                    List<Class<?>> paramTypes, List<?> args) {
        return run(restrictedCode, unrestrictedCode,
                cls.getName(), methodName, paramTypes, args);
    }

    private static void ensureStreamsInstalled() {
        if (stdIn == null) {
            synchronized (InJvmSandbox.class) {
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

    private static void ensureSecurityInstalled() {
        if (policy == null) {
            synchronized (InJvmSandbox.class) {
                if (policy == null) {
                    var pol = new SandboxPolicy();
                    Policy.setPolicy(pol);
                    System.setSecurityManager(new SecurityManager());

                    policy = pol; // signal for other threads that everything is ready
                }
            }
        }
    }

    /**
     * Runs the code in a different thread so it can be killed after the timeout.
     * This is a very hard timeout ({@link Thread#stop()}) that should be able
     * to handle pretty much anything.
     */
    private <V> V runWithTimeout(Callable<V> action) throws Exception {
        var task = new FutureTask<>(action);
        var thread = new Thread(task);
        // we should be able to kill the thread (see below), but just in
        // case, if everything else fails, the thread is set to "daemon", so
        // that it is finally killed when the JVM exists:
        thread.setDaemon(true);
        thread.start();

        while (true) {
            try {
                return task.get(MILLISECONDS.convert(timeout), MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore, try again
            } catch (ExecutionException e) {
                throw asException(e.getCause());
            } catch (TimeoutException e) {
                kill(thread);
                throw e;
            }
        }
    }

    /**
     * Uses the infamous {@link Thread#stop()} method to kill the thread.
     * And, just in case someone tries to catch the thrown
     * {@link ThreadDeath}, more of them are thrown, faster and faster.
     */
    @SuppressWarnings("deprecation")
    private void kill(Thread thread) {
        int waitTime = 100;
        do {
            thread.stop();
            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                    waitTime /= 2;
                } catch (InterruptedException ignored) {}
            }
        } while (thread.isAlive());
    }

    private static Exception asException(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof Exception) {
            return (Exception) t;
        } else { // Throwable or weird subclass of it...
            throw new AssertionError(t);
        }
    }
}
