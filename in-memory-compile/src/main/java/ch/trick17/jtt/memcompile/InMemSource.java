package ch.trick17.jtt.memcompile;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Files.readString;
import static javax.tools.JavaFileObject.Kind.SOURCE;

public class InMemSource extends SimpleJavaFileObject {

    private static final Pattern PACKAGE_NAME = Pattern.compile("\\bpackage\\s+([^\\s;]+)");
    private static final Pattern CLASS_NAME = Pattern.compile("\\bclass\\s+([^\\s{]+)");

    public static InMemSource fromFile(Path file) throws IOException {
        return new InMemSource(readString(file));
    }

    private final String content;
    private final String firstClassName;

    public InMemSource(String content) {
        super(URI.create("mem:///" + firstClassName(content).replace('.', '/') + ".java"), SOURCE);
        this.content = content;
        this.firstClassName = firstClassName(content);
    }

    private static String firstClassName(String source) {
        var simpleName = firstMatch(source, CLASS_NAME)
                .orElseThrow(() -> new IllegalArgumentException("no class name found"));
        return firstMatch(source, PACKAGE_NAME)
                .map(packageName -> packageName + "." + simpleName)
                .orElse(simpleName);
    }

    private static Optional<String> firstMatch(String code, Pattern pattern) {
        return code.lines()
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .findFirst();
    }

    public String getContent() {
        return content;
    }

    public String getFirstClassName() {
        return firstClassName;
    }

    @Override
    public String getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
