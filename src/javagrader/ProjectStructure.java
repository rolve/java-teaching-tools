package javagrader;

import java.nio.file.Path;

public enum ProjectStructure {
    ECLIPSE("src"),
    MAVEN("src/main/java");

    public final Path src;

    ProjectStructure(String srcDir) {
        this.src = Path.of(srcDir);
    }
}
