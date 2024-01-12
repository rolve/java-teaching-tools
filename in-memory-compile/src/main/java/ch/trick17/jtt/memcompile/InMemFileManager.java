package ch.trick17.jtt.memcompile;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.List.copyOf;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.groupingBy;
import static javax.tools.JavaFileObject.Kind.CLASS;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

public class InMemFileManager
        extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final Map<String, List<InMemClassFile>> memClassPath;
    private final List<InMemClassFile> outputClassFiles = new ArrayList<>();

    public InMemFileManager(ClassPath classPath) {
        super(getSystemJavaCompiler().getStandardFileManager(null, ROOT, UTF_8));
        try {
            fileManager.setLocationFromPaths(CLASS_PATH, classPath.fileClassPath());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        this.memClassPath = classPath.memClassPath().stream()
                .collect(groupingBy(InMemClassFile::getPackageName));
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
                                         Set<Kind> kinds, boolean recurse) throws IOException {
        if (location == CLASS_PATH && memClassPath.containsKey(packageName)) {
            if (recurse) {
                throw new UnsupportedOperationException();
            }
            return copyOf(memClassPath.get(packageName));
        } else {
            return super.list(location, packageName, kinds, recurse);
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               Kind kind, FileObject sibling) {
        if (location != CLASS_OUTPUT || kind != CLASS) {
            return null;
        }
        var classFile = new InMemClassFile(className);
        outputClassFiles.add(classFile);
        return classFile;
    }

    public List<InMemClassFile> getOutput() {
        return unmodifiableList(outputClassFiles);
    }
}
