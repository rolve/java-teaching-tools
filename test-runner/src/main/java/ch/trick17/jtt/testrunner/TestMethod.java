package ch.trick17.jtt.testrunner;

/**
 * Assumes that test methods are not overloaded, so that class name and method
 * name are sufficient to identify a test method. Names of nested classes use
 * '{@code .}' as separator.
 */
public record TestMethod(
        String className,
        String name) {

    public String toString() {
        return className + "." + name;
    }
}
