package ch.trick17.jtt.testrunner.forkedvm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ThrowableDeserializer extends JsonDeserializer<Throwable> {

    @Override
    public Throwable deserialize(JsonParser p, DeserializationContext c) throws IOException {
        var node = (TextNode) p.getCodec().readTree(p);
        var in = new ByteArrayInputStream(node.binaryValue());
        try {
            return (Throwable) new ObjectInputStream(in).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
