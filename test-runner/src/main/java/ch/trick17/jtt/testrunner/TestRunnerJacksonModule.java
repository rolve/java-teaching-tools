package ch.trick17.jtt.testrunner;

import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
            return deserializeTestMethod(p.getText());
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
            return deserializeTestMethod(key);
        }
    }

    private static TestMethod deserializeTestMethod(String string) {
        var withoutParams = string.replaceAll("\\(.*\\)", "");
        var methodDot = withoutParams.lastIndexOf('.');
        if (methodDot == -1) {
            throw new IllegalArgumentException("Invalid test method name (no dot): " + string);
        }
        return new TestMethod(string.substring(0, methodDot), string.substring(methodDot + 1));
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
}
