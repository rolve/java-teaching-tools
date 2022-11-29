import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ch.trick17.jtt.junitextensions.Score;

public class TestWithScore {

    @Score
    public Double score;

    @Test
    public void testWithScore() {
        assertEquals(2, Add.add(1, 1));
        if (Math.random() > 0.5) {
            score = 100.0;
        } else {
            score = 50.0;
        }
    }

    @Test
    public void testWithScoreFirst() {
        score = 100.0;
        assertEquals(2, Add.add(1, 1));
    }

    @Test
    public void testWithoutScore() {
        assertEquals(2, Add.add(1, 1));
    }

    @Test
    public void testWithOrWithoutScore() {
        assertEquals(2, Add.add(1, 1));
        if (Math.random() > 0.5) {
            score = 100.0;
        }
    }
}
