import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class TestWithDisplayNames {

    @Test
    @DisplayName("test4")
    void test1() {
        assertEquals(5, 2 + 2);
    }

    @Test
    void test2() {
        assertEquals(5, 2 + 2);
    }

    @Test
    @DisplayName("test1")
    void test3() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @DisplayName("test3")
    void test4() {
        assertEquals(4, 2 + 2);
    }
}
