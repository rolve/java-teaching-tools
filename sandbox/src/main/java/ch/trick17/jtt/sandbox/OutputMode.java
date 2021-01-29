package ch.trick17.jtt.sandbox;

public enum OutputMode {
    /**
     * No special handling. The output goes to the usual destination,
     * i.e., <code>System.out</code> or <code>System.err</code>.
     */
    NORMAL,
    /**
     * The output is discarded.
     */
    DISCARD,
    /**
     * The output is recorded and made available in the
     * {@link SandboxResult}. It does not go to the usual destination.
     */
    RECORD,
    /**
     * The output is recorded and also forwarded to the usual
     * destination.
     */
    RECORD_FORWARD;
}
