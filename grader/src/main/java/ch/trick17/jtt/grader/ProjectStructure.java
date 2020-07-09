package ch.trick17.jtt.grader;

import java.nio.file.Path;

public enum ProjectStructure {

    /**
     * Sources are located in "src" directory
     */
    ECLIPSE("src"),

    /**
     * Sources are located in "src/main/java" directory
     */
    MAVEN("src/main/java");

    public final Path srcDir;

    ProjectStructure(String srcDir) {
        this.srcDir = Path.of(srcDir);
    }
}
