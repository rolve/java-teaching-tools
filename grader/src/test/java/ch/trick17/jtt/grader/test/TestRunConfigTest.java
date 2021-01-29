package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRunConfigTest {

    private TestRunConfig config = new TestRunConfig(
            "Test",
            List.of("test", "test/more"),
            3,
            6000,
            10000,
            true);

    private String json = "{" +
            "\"testClass\":\"Test\"," +
            "\"codeUnderTestPaths\":[\"test\",\"test/more\"]," +
            "\"repetitions\":3," +
            "\"repTimeoutMillis\":6000," +
            "\"testTimeoutMillis\":10000," +
            "\"permRestrictions\":true" +
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
