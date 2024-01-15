package ch.trick17.jtt.sandbox;

public class SandboxResult<T> {

    public enum Kind {
        NORMAL, EXCEPTION, TIMEOUT, OUT_OF_MEMORY, ILLEGAL_OPERATION;
    }

    public static <T> SandboxResult<T> normal(T value) {
        return new SandboxResult<>(Kind.NORMAL, value, null);
    }

    public static <T> SandboxResult<T> exception(Throwable exception) {
        return new SandboxResult<>(Kind.EXCEPTION, null, exception);
    }

    public static <T> SandboxResult<T> timeout() {
        return new SandboxResult<>(Kind.TIMEOUT, null, null);
    }

    public static <T> SandboxResult<T> outOfMemory(OutOfMemoryError error) {
        return new SandboxResult<>(Kind.OUT_OF_MEMORY, null, error);
    }

    public static <T> SandboxResult<T> illegalOperation(SecurityException exception) {
        return new SandboxResult<>(Kind.ILLEGAL_OPERATION, null, exception);
    }

    private final Kind kind;
    private final T value;
    private final Throwable exception;
    private String stdOut = null;
    private String stdErr = null;

    private SandboxResult(Kind kind, T value, Throwable exception) {
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
}
