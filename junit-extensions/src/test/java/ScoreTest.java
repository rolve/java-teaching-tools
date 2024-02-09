import ch.trick17.jtt.junitextensions.Score;
import org.junit.jupiter.api.Test;

public class ScoreTest {

    @Score
    public Double score;

    @Test
    void withScore() {
        score = 100.0;
    }

    @Test
    void withoutScore() {

    }
}
