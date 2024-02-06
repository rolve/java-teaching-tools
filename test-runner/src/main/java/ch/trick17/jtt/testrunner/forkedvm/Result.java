package ch.trick17.jtt.testrunner.forkedvm;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_ARRAY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.MINIMAL_CLASS;

@JsonTypeInfo(use = MINIMAL_CLASS, include = WRAPPER_ARRAY)
public sealed interface Result {
    record ReturnedValue(Object value) implements Result {}
    record ThrownException(Throwable exception) implements Result {}
}
