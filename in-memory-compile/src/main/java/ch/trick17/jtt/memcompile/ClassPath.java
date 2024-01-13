package ch.trick17.jtt.memcompile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.pathSeparator;
import static java.lang.System.getProperty;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.List.copyOf;

/**
 * Combines im-memory class files and regular class path entries.
 */
public record ClassPath(
        List<InMemClassFile> memClassPath,
        List<Path> fileClassPath) {

    public static ClassPath fromMemory(List<InMemClassFile> classPath) {
        return new ClassPath(classPath, emptyList());
    }

    public static ClassPath fromFiles(List<Path> classPath) {
        return new ClassPath(emptyList(), classPath);
    }

    public static ClassPath fromCurrent() {
        var classPath = getProperty("java.class.path");
        return fromFiles(stream(classPath.split(pathSeparator))
                .map(Path::of)
                .toList());
    }

    public static ClassPath empty() {
        return new ClassPath(emptyList(), emptyList());
    }

    public ClassPath {
        memClassPath = copyOf(memClassPath);
        fileClassPath = copyOf(fileClassPath);
    }

    public ClassPath with(ClassPath other) {
        var mem = new ArrayList<>(memClassPath);
        mem.addAll(other.memClassPath);
        var files = new ArrayList<>(fileClassPath);
        files.addAll(other.fileClassPath);
        return new ClassPath(mem, files);
    }

    public ClassPath withMemory(List<InMemClassFile> classPath) {
        return with(ClassPath.fromMemory(classPath));
    }

    public ClassPath withFiles(List<Path> classPath) {
        return with(ClassPath.fromFiles(classPath));
    }

    public ClassPath withCurrent() {
        return with(ClassPath.fromCurrent());
    }
}
