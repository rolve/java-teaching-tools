package ch.trick17.jtt.testrunner.forkedvm;

import java.util.List;

public record MethodCall(
        String className,
        String methodName,
        List<String> paramTypeNames,
        List<?> args) {
}
