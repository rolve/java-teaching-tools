package ch.trick17.jtt.testrunner.forkedvm;

import ch.trick17.jtt.memcompile.InMemClassFile;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;

public class InMemClassFileDeserializer extends JsonDeserializer<InMemClassFile> {

    @Override
    public InMemClassFile deserialize(JsonParser p, DeserializationContext c) throws IOException {
        var node = p.getCodec().readTree(p);
        var className = ((TextNode) node.get("className")).asText();
        var content = ((TextNode) node.get("content")).binaryValue();
        return new InMemClassFile(className, content);
    }
}
