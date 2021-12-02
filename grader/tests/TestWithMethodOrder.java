import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class TestWithMethodOrder {

    @Test
    @Order(4)
    public void test1() {
        assertEquals(5, 2 + 2);
    }

    @Test
    @Order(3)
    public void test2() {
        assertEquals(5, 2 + 2);
    }

    @Test
    @Order(2)
    public void test3() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @Order(1)
    public void test4() {
        assertEquals(4, 2 + 2);
    }
}
