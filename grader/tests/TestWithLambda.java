import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import multiply.Multiply;

public class TestWithLambda {

    @Test
    void add() {
        // if Add is missing, the lambda will not compile, which causes all
        // other tests to fail too if test robustness is not enhanced
        assertDoesNotThrow(() -> Add.add(0, 0));
    }

    @Test
    void multiply() {
        assertEquals(1, Multiply.multiply(1, 1));
        assertEquals(4, Multiply.multiply(2, 2));
        assertEquals(18, Multiply.multiply(3, 6));
        assertEquals(18, Multiply.multiply(6, 3));
    }

    // extra tests to check that enhancing robustness does not break anything:

    @Test
    void checkedException() throws InterruptedException {
        assertDoesNotThrow(() -> Multiply.multiply(0, 1));
    }

    @Test
    void multipleCheckedExceptions() throws InterruptedException, IOException, java.sql.SQLException {
        assertDoesNotThrow(() -> Multiply.multiply(1, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void params(int b) {
        int a = 3;
        assertDoesNotThrow(() -> Multiply.multiply(a, b));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void nonFinalParams(int b) {
        int a = 3;
        b += 1; // this does not work when test code is wrapped in a class
        assertEquals(a * b, Multiply.multiply(a, b));
    }

    // TODO: Test methods that are parameterized, modify a parameter (like
    //  above), *and* contain a lambda expression don't work yet...
}
