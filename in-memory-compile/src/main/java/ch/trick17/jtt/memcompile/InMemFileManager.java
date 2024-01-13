package ch.trick17.jtt.memcompile;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.List.copyOf;
import static java.util.Locale.ROOT;
import static javax.tools.JavaFileObject.Kind.CLASS;
import static javax.tools.JavaFileObject.Kind.SOURCE;
import static javax.tools.StandardLocation.*;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

public class InMemFileManager
        extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final List<InMemSource> sources;
    private final List<InMemClassFile> memClassPath;
    private final List<InMemClassFile> outputClassFiles = new ArrayList<>();

    public InMemFileManager(List<InMemSource> sources, ClassPath classPath) {
        super(getSystemJavaCompiler().getStandardFileManager(null, ROOT, UTF_8));
        try {
            fileManager.setLocationFromPaths(CLASS_PATH, classPath.fileClassPath());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        this.sources = copyOf(sources);
        this.memClassPath = copyOf(classPath.memClassPath());
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
                                         Set<Kind> kinds, boolean recurse) throws IOException {
        if (location == CLASS_PATH && kinds.contains(CLASS)) {
            if (recurse && packageName.isEmpty()) {
                throw new UnsupportedOperationException();
            }
            var matching = memClassPath.stream()
                    .filter(f -> f.getPackageName().equals(packageName)
                                 || recurse && f.getPackageName().startsWith(packageName + "."))
                    .collect(Collectors.<JavaFileObject>toList());
            if (!matching.isEmpty()) {
                return matching;
            }
        }
        return super.list(location, packageName, kinds, recurse);
    }

    @Override
    public boolean hasLocation(Location location) {
        return location == SOURCE_PATH
               || location == CLASS_PATH
               || location == CLASS_OUTPUT
               || super.hasLocation(location);
    }

    @Override
    public boolean contains(Location location, FileObject file) {
        return location == SOURCE_PATH
               && file instanceof InMemSource
               && sources.contains(file);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className,
                                              Kind kind) throws IOException {
        if (location == CLASS_PATH && kind == CLASS) {
            var matching = memClassPath.stream()
                    .filter(f -> f.getClassName().equals(className.replace('/', '.')))
                    .findFirst();
            if (matching.isPresent()) {
                return matching.get();
            }
        }
        return super.getJavaFileForInput(location, className, kind);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               Kind kind, FileObject sibling) {
        if (location != CLASS_OUTPUT || kind != CLASS) {
            return null;
        }
        var classFile = new InMemClassFile(className.replace('/', '.'));
        outputClassFiles.add(classFile);
        return classFile;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof InMemClassFile) {
            return ((InMemClassFile) file).getClassName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }

    public List<InMemClassFile> getOutput() {
        return unmodifiableList(outputClassFiles);
    }
}
