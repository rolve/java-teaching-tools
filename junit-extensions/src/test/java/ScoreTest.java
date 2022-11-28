import ch.trick17.jtt.junitextensions.Score;
import org.junit.jupiter.api.Test;

public class ScoreTest {

    @Score
    public Double score;

    @Test
    public void testWithScore() {
        score = 100.0;
    }

    @Test
    public void testWithoutScore() {

    }
}
