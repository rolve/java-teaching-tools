package ch.trick17.jtt.grader.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.io.File.separator;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRunConfigTest {

    static final String SLASH = escapeJava(separator);

    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    TestRunConfig config = new TestRunConfig(
            "Test",
            "bin",
            "bla/test-bin",
            3,
            6000,
            10000,
            true,
            List.of("lib/foo.jar", "/lib/bar.jar"));

    String json = "{" +
            "\"testClassName\":\"Test\"," +
            "\"codeUnderTestString\":\"bin\"," +
            "\"testCodeString\":\"bla" + SLASH + "test-bin\"," +
            "\"repetitions\":3," +
            "\"repTimeoutMillis\":6000," +
            "\"testTimeoutMillis\":10000," +
            "\"permRestrictions\":true," +
            "\"dependenciesStrings\":[\"lib" + SLASH + "foo.jar\",\"" + SLASH + "lib" + SLASH + "bar.jar\"]" +
            "}";

    @Test
    public void testWrite() throws JsonProcessingException {
        assertEquals(json, mapper.writeValueAsString(config));
    }

    @Test
    public void testRead() throws JsonProcessingException {
        assertEquals(config, mapper.readValue(json, TestRunConfig.class));
    }
}
