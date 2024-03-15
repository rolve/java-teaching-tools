import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestWithParams {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 0})  // test 0 last, because this is the case that the "fails-test"
    void add(int b) {               // submission passes. If only the result of the last test run
        int a = 3;                  // were taken, that submission would appear to pass the test.
        assertEquals(a + b, Add.add(a, b));
    }
}
