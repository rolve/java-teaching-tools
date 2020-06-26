import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AddTest {

    @Test
    public void testAdd1() {
        assertEquals(0, Add.add(0, 0));
        assertEquals(2, Add.add(2, 0));
    }

    @Test
    public void testAdd2() {
        assertEquals(2, Add.add(1, 1));
        assertEquals(4, Add.add(2, 2));
        assertEquals(9, Add.add(3, 6));
        assertEquals(9, Add.add(6, 3));
    }
}
