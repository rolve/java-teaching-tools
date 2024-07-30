package poker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TryCatchTest {

    @Test
    void foo() {
        assertDoesNotThrow(() -> TryCatch.foo());
    }
}
