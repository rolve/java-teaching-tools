import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ch.trick17.jtt.junitextensions.Score;

public class TestWithScore {

    @Score
    public Double score;

    @Test
    void withScore() {
        assertEquals(2, Add.add(1, 1));
        if (Math.random() > 0.5) {
            score = 100.0;
        } else {
            score = 50.0;
        }
    }

    @Test
    void withScoreFirst() {
        score = 100.0;
        assertEquals(2, Add.add(1, 1));
    }

    @Test
    void withoutScore() {
        assertEquals(2, Add.add(1, 1));
    }

    @Test
    void withOrWithoutScore() {
        assertEquals(2, Add.add(1, 1));
        if (Math.random() > 0.5) {
            score = 100.0;
        }
    }
}
