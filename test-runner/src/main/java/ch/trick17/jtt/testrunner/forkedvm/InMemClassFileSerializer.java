package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class InMemClassFileSerializer extends JsonSerializer<InMemClassFile> {
    @Override
    public void serialize(InMemClassFile value, JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("className", value.getClassName());
        gen.writeBinaryField("content", value.getContent());
        gen.writeEndObject();
    }
}
