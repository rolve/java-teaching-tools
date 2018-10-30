import static java.util.Objects.requireNonNull;

public class Task {

    public final String projectName;
    public final String suffix;
    public final Class<?> testClass;
    public final Class<?> classUnderTest;
    public final int instrThreshold;

    public Task(String projectName, Class<?> classUnderTest, Class<?> testClass, int instrThreshold) {
        this(projectName, "", classUnderTest, testClass, instrThreshold);
    }

    public Task(String projectName, String suffix, Class<?> classUnderTest, Class<?> testClass, int instrThreshold) {
		this.projectName = requireNonNull(projectName);
        this.suffix = requireNonNull(suffix);
        this.classUnderTest = requireNonNull(classUnderTest);
        this.testClass = requireNonNull(testClass);
        this.instrThreshold = instrThreshold;
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
        if(this == obj) {
            return true;
        } else if(obj == null) {
            return false;
        } else if(!(obj instanceof Task)) {
            return false;
        }
        Task other = (Task) obj;
        if(!projectName.equals(other.projectName)) {
            return false;
        } else if(!suffix.equals(other.suffix)) {
            return false;
        }
        return true;
    }
    
    
}
