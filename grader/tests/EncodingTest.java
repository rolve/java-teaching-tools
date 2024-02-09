import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodingTest {

    @Test
    void encoding() throws IOException {
        var original = "Neuchâtel";
        var in = new ByteArrayInputStream(original.getBytes(UTF_8));
        var decoded = Decoder.decode(in);
        assertEquals(original, decoded);
    }
}
