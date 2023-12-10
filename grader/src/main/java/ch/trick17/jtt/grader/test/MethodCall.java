package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static java.util.List.copyOf;

public class MethodCall {
    private final String className;
    private final String methodName;
    private final List<String> paramTypeNames;
    private final List<?> args;

    public MethodCall(String className, String methodName,
                      List<String> paramTypeNames, List<?> args) {
        this.className = className;
        this.methodName = methodName;
        this.paramTypeNames = copyOf(paramTypeNames);
        this.args = copyOf(args);
    }

    @JsonProperty
    public String className() {
        return className;
    }

    @JsonProperty
    public String methodName() {
        return methodName;
    }

    @JsonProperty
    public List<String> paramTypeNames() {
        return paramTypeNames;
    }

    @JsonProperty
    public List<?> args() {
        return args;
    }
}
