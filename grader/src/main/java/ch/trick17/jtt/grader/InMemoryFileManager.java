package ch.trick17.jtt.grader;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static java.util.Locale.ROOT;
import static javax.tools.JavaFileObject.Kind.CLASS;
import static javax.tools.JavaFileObject.Kind.SOURCE;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

public class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Map<String, String> sourceFiles;
    private final Map<String, byte[]> classFiles = new HashMap<>();

    public InMemoryFileManager(Map<String, String> sourceFiles) {
        super(getSystemJavaCompiler().getStandardFileManager(null, ROOT, UTF_8));
        this.sourceFiles = sourceFiles;
    }

    public Iterable<JavaFileObject> getSourceFiles() {
        return sourceFiles.keySet().stream()
                .map(this::getSourceFile)
                .toList();
    }

    private JavaFileObject getSourceFile(String className) {
        var uri = URI.create("mem:///" + className.replace('.', '/') + ".java");
        return new SimpleJavaFileObject(uri, SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sourceFiles.get(className);
            }
        };
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               Kind kind, FileObject sibling) {
        if (location != CLASS_OUTPUT || kind != CLASS) {
            return null;
        }
        var uri = URI.create("mem:///" + className.replace('.', '/') + ".class");
        return new SimpleJavaFileObject(uri, CLASS) {
            @Override
            public OutputStream openOutputStream() {
                return new ByteArrayOutputStream() {
                    @Override
                    public void close() {
                        classFiles.put(className, toByteArray());
                    }
                };
            }
        };
    }

    public Map<String, byte[]> getClassFiles() {
        return unmodifiableMap(classFiles);
    }
}
