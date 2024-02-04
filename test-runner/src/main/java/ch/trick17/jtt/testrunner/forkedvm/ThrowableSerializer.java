package ch.trick17.jtt.testrunner.forkedvm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ThrowableSerializer extends JsonSerializer<Throwable> {

    @Override
    public void serialize(Throwable value, JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        var out = new ByteArrayOutputStream();
        new ObjectOutputStream(out).writeObject(value);
        gen.writeBinary(out.toByteArray());
    }
}
