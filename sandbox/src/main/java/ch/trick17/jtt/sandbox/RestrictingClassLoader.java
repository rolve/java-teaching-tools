package ch.trick17.jtt.sandbox;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class RestrictingClassLoader extends ClassLoader {

    private final ClassPool pool = new ClassPool(true);
    private final Whitelist permittedCalls;
    private final Set<String> restrictedClasses;

    public RestrictingClassLoader(List<Path> restrictedCode,
                                  List<Path> unrestrictedCode,
                                  Whitelist permittedCalls) throws IOException {
        super(createParent(unrestrictedCode));
        try {
            for (var path : restrictedCode) {
                pool.appendClassPath(path.toString());
            }
            for (var path : unrestrictedCode) {
                pool.appendClassPath(path.toString());
            }
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        this.permittedCalls = permittedCalls;

        restrictedClasses = new HashSet<>();
        for (var path : restrictedCode) {
            try (var walk = Files.walk(path)) {
                walk.map(Path::toString)
                        .filter(p -> p.endsWith(".class"))
                        .map(name -> name.substring(path.toString().length() + 1))
                        .map(name -> name.substring(0, name.length() - 6))
                        .map(name -> name.replace(path.getFileSystem().getSeparator(), "."))
                        .forEach(restrictedClasses::add);
            }
        }
    }

    private static ClassLoader createParent(List<Path> unrestrictedCode) {
        var urls = unrestrictedCode.stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(URL[]::new);
        return new URLClassLoader(urls, getPlatformClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // if the class is in the restricted set, load it with this class loader
        if (restrictedClasses.contains(name)) {
            return findClass(name);
        }

        // otherwise, use standard class loading delegation...
        var cls = super.loadClass(name, resolve);
        if (cls.getClassLoader() == null) {
            return cls;
        }

        // ... but, to ensure that classes referred to by the loaded class are
        // also loaded by this class loader (and not the parent), reload the
        // class with this class loader
        var classFile = "/" + name.replace('.', '/') + ".class";
        try (var in = cls.getResourceAsStream(classFile)) {
            var bytecode = requireNonNull(in).readAllBytes();
            return defineClass(name, bytecode, 0, bytecode.length);
        } catch (NullPointerException | IOException e) {
            throw new AssertionError(name, e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        CtClass cls;
        try {
            cls = pool.get(name);
        } catch (NotFoundException e) {
            throw new ClassNotFoundException("class not found in pool", e);
        }
        var editor = new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                var cls = m.getClassName();
                var method = m.getMethodName();
                if (!restrictedClasses.contains(cls) &&
                    !permittedCalls.methodPermitted(cls, method)) {
                    m.replace(createThrows(cls, method));
                }
            }

            public void edit(NewExpr e) throws CannotCompileException {
                var cls = e.getClassName();
                if (!restrictedClasses.contains(cls) &&
                    !permittedCalls.constructorPermitted(cls)) {
                    e.replace(createThrows(cls, "<init>"));
                }
            }

            private String createThrows(String className, String methodName) {
                return """
                        {
                            if (true) { // weirdly, doesn't work without this
                                throw new SecurityException("Illegal call: %s");
                            }
                            $_ = $proceed($$);
                        }""".formatted(className + "." + methodName);
            }
        };
        try {
            for (var constructor : cls.getDeclaredConstructors()) {
                constructor.instrument(editor);
            }
            for (var method : cls.getDeclaredMethods()) {
                method.instrument(editor);
            }
            var bytecode = cls.toBytecode();
            return defineClass(name, bytecode, 0, bytecode.length);
        } catch (CannotCompileException | IOException e) {
            throw new AssertionError(e);
        }
    }
}
