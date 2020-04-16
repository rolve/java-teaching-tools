package javagrader;

import java.nio.file.Path;

public enum ProjectStructure {
    ECLIPSE("src", "bin"),
    MAVEN("src/main/java", "target/classes");

    public final Path src;
    public final Path bin;

    ProjectStructure(String srcDir, String binDir) {
        this.src = Path.of(srcDir);
        this.bin = Path.of(binDir);
    }
}
