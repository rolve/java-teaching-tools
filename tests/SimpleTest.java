import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SimpleTest {

    @Test
    public void testAdd1() {
        assertEquals(0, Simple.add(0, 0));
        assertEquals(2, Simple.add(2, 0));
    }

    @Test
    public void testAdd2() {
        assertEquals(2, Simple.add(1, 1));
        assertEquals(4, Simple.add(2, 2));
        assertEquals(9, Simple.add(3, 6));
        assertEquals(9, Simple.add(6, 3));
    }
}
