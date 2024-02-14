import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

public class TestWithOrderAnnotations {

    @Test
    @Order(4)
    void test1() {
        assertEquals(5, 2 + 2);
    }

    @Test
    @Order(3)
    void test2() {
        assertEquals(5, 2 + 2);
    }

    @Test
    @Order(2)
    void test3() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @Order(1)
    void test4() {
        assertEquals(4, 2 + 2);
    }
}
