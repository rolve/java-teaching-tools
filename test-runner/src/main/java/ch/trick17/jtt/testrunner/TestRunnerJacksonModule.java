package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.ValueWrapper;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

@SuppressWarnings("unused")
public class TestRunnerJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(TestMethod.class, TestMethodMixin.class);
        context.setMixInAnnotations(InMemClassFile.class, InMemClassFileMixin.class);
        context.setMixInAnnotations(Throwable.class, ThrowableMixin.class);
        context.setMixInAnnotations(StackTraceElement.class, StackTraceElementMixin.class);
        context.setMixInAnnotations(ClassNotFoundException.class, ClassNotFoundExceptionMixin.class);
        context.setMixInAnnotations(AssertionFailedError.class, AssertionFailedErrorMixin.class);
        context.setMixInAnnotations(ValueWrapper.class, ValueWrapperMixin.class);
    }

    @JsonSerialize(using = TestMethodSerializer.class, keyUsing = TestMethodKeySerializer.class)
    @JsonDeserialize(using = TestMethodDeserializer.class, keyUsing = TestMethodKeyDeserializer.class)
    public static class TestMethodMixin {}

    public static class TestMethodSerializer extends JsonSerializer<TestMethod> {
        public void serialize(TestMethod value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeString(value.className() + "." + value.name());
        }
    }

    public static class TestMethodDeserializer extends JsonDeserializer<TestMethod> {
        public TestMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var text = p.getText();
            var lastDot = text.lastIndexOf('.');
            if (lastDot == -1) {
                throw new IllegalArgumentException("Invalid test method name (no dot): " + text);
            }
            return new TestMethod(text.substring(0, lastDot), text.substring(lastDot + 1));
        }
    }

    public static class TestMethodKeySerializer extends JsonSerializer<TestMethod> {
        public void serialize(TestMethod value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeFieldName(value.className() + "." + value.name());
        }
    }

    public static class TestMethodKeyDeserializer extends KeyDeserializer {
        public TestMethod deserializeKey(String key, DeserializationContext context) {
            var lastDot = key.lastIndexOf('.');
            if (lastDot == -1) {
                throw new IllegalArgumentException("Invalid test method name (no dot): " + key);
            }
            return new TestMethod(key.substring(0, lastDot), key.substring(lastDot + 1));
        }
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
