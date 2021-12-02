import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class TestWithDisplayNames {

    @Test
    @DisplayName("test4")
    public void test1() {
        assertEquals(5, 2 + 2);
    }

    @Test
    public void test2() {
        assertEquals(5, 2 + 2);
    }

    @Test
    @DisplayName("test1")
    public void test3() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @DisplayName("test3")
    public void test4() {
        assertEquals(4, 2 + 2);
    }
}
