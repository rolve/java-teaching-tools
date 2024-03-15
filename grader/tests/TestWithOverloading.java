import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestWithOverloading {

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void add(int b) {
        int a = 3;
        assertEquals(a + b, Add.add(a, b));
    }

    @Test
    void add() {
        assertEquals(3, Add.add(3, 0));
        assertEquals(3, Add.add(0, 3));
    }
}
