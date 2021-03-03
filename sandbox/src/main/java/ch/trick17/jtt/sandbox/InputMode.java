package ch.trick17.jtt.sandbox;

public enum InputMode {
    /**
     * No special handling. The input is read from the usual source,
     * i.e., <code>System.in</code>.
     */
    NORMAL,
    /**
     * No input is provided; the original <code>System.in</code> is
     * unaffected by the sandboxed code.
     */
    EMPTY,
    /**
     * No input is provided, like with <code>EMPTY</code>. In
     * addition, the stream installed as <code>System.in</code> is
     * closed before the sandboxed code is executed.
     */
    CLOSED;

    // TODO: Add option to provide predefined input
}
