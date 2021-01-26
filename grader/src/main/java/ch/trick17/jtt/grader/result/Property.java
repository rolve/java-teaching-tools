package ch.trick17.jtt.grader.result;

public enum Property {

    /**
     * The submission was compiled. This either means that there were no compile
     * errors or that the {@linkplain Compiler#ECLIPSE Eclipse compiler} was
     * used, which can often compile erroneous code.
     */
    COMPILED,

    /**
     * The submission contains compile errors. This does not necessarily mean
     * that it cannot be tested; the {@linkplain Compiler#ECLIPSE Eclipse
     * compiler} can often compile erroneous code. Testing such code may lead to
     * exceptions at runtime, but only if the erroneous part of the code is
     * actually reached during execution. This means that some or all tests may
     * be passed nonetheless.
     */
    COMPILE_ERRORS,

    /**
     * The submission produced different outcomes for at least one test when
     * executed multiple times.
     */
    NONDETERMINISTIC,

    /**
     * At least one test execution timed out. Such a test is considered failed.
     */
    TIMEOUT,

    /**
     * Not all repetitions of a test were executed, due to a long running time.
     * This may happen even though no single test execution timed out, because
     * of an additional timeout for all repetitions together. However, this
     * usually happens for non-terminating tests, in which case also the
     * {@link #TIMEOUT} property is present.
     */
    INCOMPLETE_REPETITIONS,

    /**
     * Tried to perform an operation that is illegal under the installed
     * security policy, such as accessing the file system or a system property.
     */
    ILLEGAL_OPERATION;

    public String prettyName() {
        return name().toLowerCase().replace('_', ' ');
    }
}
