package ch.trick17.jtt.testrunner;

import java.util.List;
import java.util.Objects;

public record ExceptionDescription(
        String className,
        String message,
        ExceptionDescription cause,
        List<StackTraceElement> stackTrace) {

    public static ExceptionDescription of(Throwable throwable) {
        var cause = throwable.getCause() != null ? ExceptionDescription.of(throwable.getCause()) : null;
        return new ExceptionDescription(throwable.getClass().getName(),
                throwable.getMessage(), cause, List.of(throwable.getStackTrace()));
    }

    public String simpleClassName() {
        var lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    @Override
    public String toString() {
        return className + ": " + message;
    }

    @Override
    public boolean equals(Object o) {
        // only take className and message into account for now, so only one
        // exception per class and message is reported in the test results
        return o instanceof ExceptionDescription other
               && className.equals(other.className)
               && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, message);
    }

    public ExceptionDescription with(String message, ExceptionDescription cause,
                                     List<StackTraceElement> stackTrace) {
        return new ExceptionDescription(className, message, cause, stackTrace);
    }
}
