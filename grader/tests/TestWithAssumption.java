import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

public class TestWithAssumption {

    @Test
    void normal() {
        assertEquals(0, Add.add(0, 0));
        assertEquals(2, Add.add(2, 0));
    }

    @Test
    void failedAssumption() {
        assumeTrue(false);
    }
}
