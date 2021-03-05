import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

public class DisabledTest {

    @Test
    public void testNormal() {
        assertEquals(0, Add.add(0, 0));
        assertEquals(2, Add.add(2, 0));
    }

    @Test
    @Disabled
    public void testDisabled() {
        throw new AssertionError();
    }
}
