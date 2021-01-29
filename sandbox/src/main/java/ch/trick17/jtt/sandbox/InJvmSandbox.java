package ch.trick17.jtt.sandbox;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Policy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Stream.concat;

public class InJvmSandbox {

    private static volatile SandboxPolicy policy;

    private boolean permRestrictions = true;
    private boolean staticStateIsolation = true;
    private Duration timeout = null;

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

        try {
            var result = restricted.call();
            return SandboxResult.normal(result);
        } catch (TimeoutException e) {
            return SandboxResult.timeout();
        } catch (SecurityException e) {
            return SandboxResult.illegalOperation(e);
        } catch (Throwable e) {
            return SandboxResult.exception(e);
        }
    }

    public <T> SandboxResult<T> run(List<URL> restrictedCode, List<URL> unrestrictedCode,
                                    Class<?> cls, String methodName,
                                    List<Class<?>> paramTypes, List<?> args) {
        return run(restrictedCode, unrestrictedCode,
                cls.getName(), methodName, paramTypes, args);
    }

    private static void ensureSecurityInstalled() {
        if (policy == null) {
            synchronized (InJvmSandbox.class) {
                if (policy == null) {
                    policy = new SandboxPolicy();
                    Policy.setPolicy(policy);
                    System.setSecurityManager(new SecurityManager());
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
