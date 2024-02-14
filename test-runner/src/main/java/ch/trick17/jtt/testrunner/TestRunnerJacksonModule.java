package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.ValueWrapper;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

@SuppressWarnings("unused")
public class TestRunnerJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(InMemClassFile.class, InMemClassFileMixin.class);
        context.setMixInAnnotations(Throwable.class, ThrowableMixin.class);
        context.setMixInAnnotations(StackTraceElement.class, StackTraceElementMixin.class);
        context.setMixInAnnotations(ClassNotFoundException.class, ClassNotFoundExceptionMixin.class);
        context.setMixInAnnotations(AssertionFailedError.class, AssertionFailedErrorMixin.class);
        context.setMixInAnnotations(ValueWrapper.class, ValueWrapperMixin.class);
    }

    @JsonIncludeProperties({"className", "content"})
    public static class InMemClassFileMixin {}

    @JsonTypeInfo(use = CLASS)
    @JsonIncludeProperties({"message", "cause", "stackTrace"})
    public static class ThrowableMixin {}

    @JsonIncludeProperties({"methodName", "className", "fileName", "lineNumber"})
    public static class StackTraceElementMixin {}

    @JsonIgnoreProperties("exception") // alias for cause
    public static class ClassNotFoundExceptionMixin {
        // need to use this constructor, otherwise 'cause' will be initialized
        // to 'null' by the default constructor (instead of to 'this') and
        // Jackson will call 'initCause' with 'null', leading to an error
        @JsonCreator
        public ClassNotFoundExceptionMixin(
                @JsonProperty("message") String message,
                @JsonProperty("cause") Throwable cause) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssertionFailedErrorMixin {
        // see above
        @JsonCreator
        public AssertionFailedErrorMixin(
                @JsonProperty("message") String message,
                @JsonProperty("cause") Throwable cause) {}
    }

    @JsonIncludeProperties({"value", "stringRepresentation"})
    public static class ValueWrapperMixin {
        @JsonCreator
        public static ValueWrapperMixin create(
                @JsonProperty("value") Object o,
                @JsonProperty("stringRepresentation") String s) {
            return null;
        }
    }
}
