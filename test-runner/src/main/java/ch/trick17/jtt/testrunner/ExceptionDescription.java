package ch.trick17.jtt.testrunner;

import java.util.List;

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
}
