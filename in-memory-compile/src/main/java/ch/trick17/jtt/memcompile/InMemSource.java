package ch.trick17.jtt.memcompile;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17;
import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;
import static java.io.File.separatorChar;
import static java.nio.file.Files.readString;
import static java.util.Comparator.comparing;
import static javax.tools.JavaFileObject.Kind.SOURCE;

public class InMemSource extends SimpleJavaFileObject {

    private static final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(JAVA_17)
            .setIgnoreAnnotationsWhenAttributingComments(true));

    public static InMemSource fromString(String source) {
        source = source.replace("\r\n", "\n");
        var parseResult = parser.parse(source);
        if (parseResult.getResult().isEmpty()) {
            var problems = parseResult.getProblems().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid Java source: " + problems);
        }
        var compilationUnit = parseResult.getResult().get();
        var typeName = compilationUnit.getTypes().stream()
                .max(comparing(t -> t.hasModifier(PUBLIC)))
                .flatMap(t -> t.getFullyQualifiedName())
                .orElseThrow(() -> new IllegalArgumentException("No type found in source"));
        var path = typeName.replace('.', '/') + ".java";
        return new InMemSource(path, source, compilationUnit);
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
    private volatile CompilationUnit parsed; // null if not parsed

    public InMemSource(String path, String content) {
        this(path, content, null);
    }

    public InMemSource(String path, String content, CompilationUnit parsed) {
        super(URI.create("mem:///" + path), SOURCE);
        if (path.contains("\\")) {
            throw new IllegalArgumentException("Path must use / separator: " + path);
        } else if (!path.endsWith(".java")) {
            throw new IllegalArgumentException("Path must end with .java: " + path);
        }
        this.path = path;
        this.content = content;
        this.parsed = parsed;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public CompilationUnit getParsed() {
        if (parsed == null) { // race condition is ok
            parsed = parser.parse(content).getResult().orElseThrow();
        }
        return parsed;
    }

    @Override
    public String getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
