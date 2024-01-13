package ch.trick17.jtt.memcompile;

import com.github.javaparser.StaticJavaParser;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;
import static java.io.File.separatorChar;
import static java.nio.file.Files.readString;
import static java.util.Comparator.comparing;
import static javax.tools.JavaFileObject.Kind.SOURCE;

public class InMemSource extends SimpleJavaFileObject {

    public static InMemSource fromString(String source) {
        var compilationUnit = StaticJavaParser.parse(source);
        var typeName = compilationUnit.getTypes().stream()
                .max(comparing(t -> t.hasModifier(PUBLIC)))
                .flatMap(t -> t.getFullyQualifiedName())
                .orElseThrow(() -> new IllegalArgumentException("No type found in source"));
        var path = typeName.replace('.', '/') + ".java";
        return new InMemSource(path, source);
    }

    public static InMemSource fromFile(Path file, Path sourceDir) throws IOException {
        if (!file.startsWith(sourceDir)) {
            throw new IllegalArgumentException("File " + file + " is not in source directory " + sourceDir);
        }
        var path = sourceDir.relativize(file).toString()
                .replace(separatorChar, '/');
        return new InMemSource(path, readString(file));
    }

    private final String path; // relative to source directory, e.g., students/Student.java
    private final String content;

    public InMemSource(String path, String content) {
        super(URI.create("mem:///" + path), SOURCE);
        if (path.contains("\\")) {
            throw new IllegalArgumentException("Path must use / separator: " + path);
        } else if (!path.endsWith(".java")) {
            throw new IllegalArgumentException("Path must end with .java: " + path);
        }
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
