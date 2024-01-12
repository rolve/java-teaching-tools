package ch.trick17.jtt.memcompile;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class InMemClassLoader extends URLClassLoader {
    private final Map<String, InMemClassFile> memClassPath;

    public InMemClassLoader(ClassPath classPath, ClassLoader parent) {
        super(classPath.fileClassPath().stream()
                .map(p -> toUrl(p))
                .toArray(URL[]::new), parent);
        this.memClassPath = classPath.memClassPath().stream()
                .collect(toMap(InMemClassFile::getClassName, identity()));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if (memClassPath.containsKey(name)) {
            var bytes = memClassPath.get(name).getContent();
            return defineClass(name, bytes, 0, bytes.length);
        } else {
            return super.loadClass(name, resolve);
        }
    }

    private static URL toUrl(Path p) {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
}
