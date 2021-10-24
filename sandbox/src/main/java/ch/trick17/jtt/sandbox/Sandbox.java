package ch.trick17.jtt.sandbox;

import java.io.PrintStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static ch.trick17.jtt.sandbox.OutputMode.NORMAL;
import static java.util.Objects.requireNonNull;

public abstract class Sandbox {

    protected boolean permRestrictions = true;
    protected Duration timeout = null;
    protected InputMode stdInMode = InputMode.NORMAL;
    protected OutputMode stdOutMode = NORMAL;
    protected OutputMode stdErrMode = NORMAL;

    /**
     * Enables or disables permission restrictions. If enabled, a
     * {@link SecurityManager} will restrict the permissions for the
     * code specified as 'restricted' when the
     * {@link #run(List, List, String, String, List, List, Class)} method
     * is called. By default, restrictions are enabled.
     */
    public Sandbox permRestrictions(boolean permRestrictions) {
        this.permRestrictions = permRestrictions;
        return this;
    }

    /**
     * Sets a timeout for the code to be executed. If a timeout is set,
     * the code is executed in a different thread that is forcefully
     * terminated when the timeout is over. By default, the timeout is
     * set to <code>null</code>, meaning it is disabled.
     */
    public Sandbox timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Sandbox stdInMode(InputMode stdInMode) {
        this.stdInMode = stdInMode;
        return this;
    }

    /**
     * Determines how to handle output to <code>System.out</code>. The
     * default mode is {@link OutputMode#NORMAL}. Note that the sandboxed
     * code may affect the I/O behavior using {@link System#setOut(PrintStream)},
     * unless it runs with restricted permissions.
     */
    public Sandbox stdOutMode(OutputMode stdOutMode) {
        this.stdOutMode = requireNonNull(stdOutMode);
        return this;
    }

    /**
     * Determines how to handle output to <code>System.err</code>. The
     * default mode is {@link OutputMode#NORMAL}. Note that the sandboxed
     * code may affect the I/O behavior using {@link System#setErr(PrintStream)},
     * unless it runs with restricted permissions.
     */
    public Sandbox stdErrMode(OutputMode stdErrMode) {
        this.stdErrMode = requireNonNull(stdErrMode);
        return this;
    }

    public abstract <T> SandboxResult<T> run(List<URL> restrictedCode, List<URL> unrestrictedCode,
                                             String className, String methodName,
                                             List<Class<?>> paramTypes, List<?> args, Class<T> resultType);

    public <T> SandboxResult<T> run(List<URL> restrictedCode, List<URL> unrestrictedCode,
                                     Class<?> cls, String methodName,
                                     List<Class<?>> paramTypes, List<?> args, Class<T> resultType) {
        return run(restrictedCode, unrestrictedCode, cls.getName(), methodName, paramTypes, args, resultType);
    }
}
