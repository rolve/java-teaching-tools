public class Task {

    public final String projectName;
    public final Class<?> testClass;
    public final int instrThreshold;

    public Task(String projectName, Class<?> testClass, int instrThreshold) {
        this.projectName = projectName;
        this.testClass = testClass;
        this.instrThreshold = instrThreshold;
    }
}
