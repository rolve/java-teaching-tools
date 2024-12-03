import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestWithOutputRecording {

    PrintStream originalOut;
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    @BeforeEach
    void setup() {
        originalOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void cleanup() {
        System.setOut(originalOut);
    }

    @Test
    void addZero() {
        AddAndPrint.add(0, 0);
        assertEquals("0" + System.lineSeparator(), out.toString());
    }

    @Test
    void addNonZero() {
        AddAndPrint.add(1, 2);
        assertEquals("3" + System.lineSeparator(), out.toString());
    }
}
