import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.MethodOrderer.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(DisplayName.class) // overrides default that takes @Order into account
public class TestWithMethodOrder {

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
