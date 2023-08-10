package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.io.File.separator;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRunConfigTest {

    private final TestRunConfig config = new TestRunConfig(
            "Test",
            List.of("test", "test/more"),
            3,
            6000,
            10000,
            true,
            List.of("lib/foo.jar", "lib/bar.jar"));

    private final String json = "{" +
            "\"testClassName\":\"Test\"," +
            "\"codeUnderTestPaths\":[\"test\",\"test/more\"]," +
            "\"repetitions\":3," +
            "\"repTimeoutMillis\":6000," +
            "\"testTimeoutMillis\":10000," +
            "\"permRestrictions\":true," +
            "\"dependenciesPaths\":[\"lib/foo.jar\",\"lib/bar.jar\"]" +
            "}";

    @Test
    public void testToJson() throws JsonProcessingException {
        assertEquals(json, config.toJson());
    }

    @Test
    public void testFromJson() throws JsonProcessingException {
        assertEquals(config, TestRunConfig.fromJson(json));
    }
}
