import static java.lang.reflect.Modifier.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestWithReflection {

    @Test
    void signature() throws NoSuchMethodException {
        assertTrue(isPublic(Add.class.getModifiers()));

        var add = Add.class.getDeclaredMethod("add", int.class, int.class);
        assertTrue(isPublic(add.getModifiers()), "method not public");
        assertTrue(isStatic(add.getModifiers()), "method not static");
    }

    @Test
    void functionality() throws Exception {
        var add = Add.class.getDeclaredMethod("add", int.class, int.class);
        add.setAccessible(true);
        int result = (int) add.invoke(null, 2, 3);
        assertEquals(5, result);
    }
}
