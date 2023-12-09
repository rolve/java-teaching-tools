package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.io.File.separator;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRunConfigTest {

    static final String SLASH = escapeJava(separator);

    private final TestRunConfig config = new TestRunConfig(
            "Test",
            List.of("test", "test/more"),
            3,
            6000,
            10000,
            true,
            List.of("lib/foo.jar", "/lib/bar.jar"));

    private final String json = "{" +
            "\"testClassName\":\"Test\"," +
            "\"codeUnderTestStrings\":[\"test\",\"test" + SLASH + "more\"]," +
            "\"repetitions\":3," +
            "\"repTimeoutMillis\":6000," +
            "\"testTimeoutMillis\":10000," +
            "\"permRestrictions\":true," +
            "\"dependenciesStrings\":[\"lib" + SLASH + "foo.jar\",\"" + SLASH + "lib" + SLASH + "bar.jar\"]" +
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
