import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class TestWithMethodSource {

    public static Stream<Arguments> add() {
        return Stream.of(
                Arguments.of(1, 2),
                Arguments.of(2, 1),
                Arguments.of(0, 1),
                Arguments.of(1, 0));
    }

    @ParameterizedTest
    @MethodSource
    void add(int a, int b) {
        assertEquals(a + b, Add.add(a, b));
    }
}
