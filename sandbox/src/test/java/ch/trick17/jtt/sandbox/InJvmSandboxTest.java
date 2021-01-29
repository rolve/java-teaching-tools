package ch.trick17.jtt.sandbox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Collections;

import static ch.trick17.jtt.sandbox.OutputMode.*;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InJvmSandboxTest {

    // Much features are tested indirectly in GraderTest, not here

    private static ByteArrayOutputStream outRecorder;
    private static ByteArrayOutputStream errRecorder;

    @BeforeAll
    public static void install() {
        outRecorder = new ByteArrayOutputStream();
        errRecorder = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outRecorder));
        System.setErr(new PrintStream(errRecorder));
    }

    @BeforeEach
    public void reset() {
        outRecorder.reset();
        errRecorder.reset();
    }
    
    @Test
    public void testOutputModeNormal() {
        var sandbox = new InJvmSandbox()
                .permRestrictions(false)
                .staticStateIsolation(false)
                .stdOutMode(NORMAL).stdErrMode(NORMAL);
        var result = sandbox.run(emptyList(), emptyList(), TestCode.class, "run",
                emptyList(), emptyList());

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    @Test
    public void testOutputModeDiscard() {
        var sandbox = new InJvmSandbox()
                .permRestrictions(false)
                .staticStateIsolation(false)
                .stdOutMode(DISCARD).stdErrMode(DISCARD);
        var result = sandbox.run(emptyList(), emptyList(), TestCode.class, "run",
                emptyList(), emptyList());

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecord() {
        var sandbox = new InJvmSandbox()
                .permRestrictions(false)
                .staticStateIsolation(false)
                .stdOutMode(RECORD).stdErrMode(RECORD);
        var result = sandbox.run(emptyList(), emptyList(), TestCode.class, "run",
                emptyList(), emptyList());

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordTimeout() {
        var sandbox = new InJvmSandbox()
                .permRestrictions(false)
                .staticStateIsolation(false)
                .timeout(Duration.ofSeconds(1))
                .stdOutMode(RECORD).stdErrMode(RECORD);
        var result = sandbox.run(emptyList(), emptyList(), TestCode.class, "run",
                emptyList(), emptyList());

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordForward() {
        var sandbox = new InJvmSandbox()
                .permRestrictions(false)
                .staticStateIsolation(false)
                .stdOutMode(RECORD_FORWARD).stdErrMode(RECORD_FORWARD);
        var result = sandbox.run(emptyList(), emptyList(), TestCode.class, "run",
                emptyList(), emptyList());

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    public static class TestCode {
        public static void run() {
            System.out.print("This goes out");
            System.err.print("This goes err");
        }
    }
}
