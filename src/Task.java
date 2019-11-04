import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

public class Task {

    public final String projectName;
    public final String suffix;
    public final Class<?> testClass;
    public final Class<?> classUnderTest;
    public final int instrThreshold;
    public final Set<String> filesToCopy;

    public Task(String projectName, Class<?> classUnderTest, Class<?> testClass,
            String... moreFilesToCopy) {
        this(projectName, "", classUnderTest, testClass, MAX_VALUE, moreFilesToCopy);
    }

    public Task(String projectName, String suffix, Class<?> classUnderTest,
            Class<?> testClass, int instrThreshold, String... moreFilesToCopy) {
        this.projectName = requireNonNull(projectName);
        this.suffix = requireNonNull(suffix);
        this.classUnderTest = requireNonNull(classUnderTest);
        this.testClass = requireNonNull(testClass);
        this.instrThreshold = instrThreshold;

        filesToCopy = new HashSet<>(asList(moreFilesToCopy));
        filesToCopy.add(testClass.getName() + ".java");
    }

    public String resultFileName() {
        return projectName + suffix + "-results.tsv";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + projectName.hashCode();
        result = 31 * result + suffix.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Task)) {
            return false;
        }
        Task other = (Task) obj;
        if (!projectName.equals(other.projectName)) {
            return false;
        } else if (!suffix.equals(other.suffix)) {
            return false;
        }
        return true;
    }
}
