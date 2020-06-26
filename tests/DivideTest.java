import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DivideTest {

    @Test
    public void testDivide() {
        assertEquals(1, Divide.divide(1, 1));
        assertEquals(1, Divide.divide(2, 2));
        assertEquals(0, Divide.divide(3, 6));
        assertEquals(2, Divide.divide(6, 3));
    }
}
