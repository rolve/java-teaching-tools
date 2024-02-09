package multiply;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MultiplyTest {

    @Test
    void multiply1() {
        assertEquals(0, Multiply.multiply(0, 0));
        assertEquals(0, Multiply.multiply(2, 0));
    }

    @Test
    void multiply2() {
        assertEquals(1, Multiply.multiply(1, 1));
        assertEquals(4, Multiply.multiply(2, 2));
        assertEquals(18, Multiply.multiply(3, 6));
        assertEquals(18, Multiply.multiply(6, 3));
    }
}
