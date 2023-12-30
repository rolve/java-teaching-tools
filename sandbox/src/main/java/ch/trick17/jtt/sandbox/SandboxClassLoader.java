package ch.trick17.jtt.sandbox;

import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute.Type;
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

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static javassist.bytecode.SignatureAttribute.toMethodSignature;

/**
 *
 */
public class SandboxClassLoader extends URLClassLoader {

    private final ClassPool pool = new ClassPool(true);
    private final Whitelist permittedCalls;
    private final Set<String> restrictedClasses;

    public SandboxClassLoader(List<Path> restrictedCode,
                              List<Path> unrestrictedCode,
                              Whitelist permittedCalls,
                              ClassLoader parent) throws IOException {
        super(toUrls(unrestrictedCode), parent);
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

    private static URL[] toUrls(List<Path> paths) {
        return paths.stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(URL[]::new);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!restrictedClasses.contains(name)) {
            return super.findClass(name);
        }

        CtClass cls;
        try {
            cls = pool.get(name);
        } catch (NotFoundException e) {
            throw new ClassNotFoundException("class not found in pool", e);
        }
        try {
            if (permittedCalls != null) {
                for (var b : cls.getDeclaredBehaviors()) {
                    b.instrument(new RestrictionsAdder());
                }
            }
            var bytecode = cls.toBytecode();
            return defineClass(name, bytecode, 0, bytecode.length);
        } catch (CannotCompileException | IOException e) {
            throw new AssertionError(e);
        }
    }

    private class RestrictionsAdder extends ExprEditor {
        public void edit(MethodCall m) throws CannotCompileException {
            try {
                var cls = m.getClassName();
                var method = m.getMethodName();
                var sig = toMethodSignature(m.getSignature());
                var paramTypes = stream(sig.getParameterTypes())
                        .map(Type::toString)
                        .toList();
                if (!restrictedClasses.contains(cls) &&
                    !permittedCalls.methodPermitted(cls, method, paramTypes)) {
                    var params = "(" + join(",", paramTypes) + ")";
                    m.replace(createThrows(
                            "Illegal call: " + cls + "." + method + params));
                }
            } catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }

        public void edit(NewExpr e) throws CannotCompileException {
            try {
                var cls = e.getClassName();
                var sig = toMethodSignature(e.getSignature());
                var paramTypes = stream(sig.getParameterTypes())
                        .map(Type::toString)
                        .toList();
                if (!restrictedClasses.contains(cls) &&
                    !permittedCalls.constructorPermitted(cls, paramTypes)) {
                    var params = "(" + join(",", paramTypes) + ")";
                    e.replace(createThrows(
                            "Illegal constructor call: new " + cls + params));
                }
            } catch (BadBytecode bb) {
                throw new CannotCompileException(bb);
            }
        }

        private String createThrows(String message) {
            return """
                    if (true) { // weirdly, doesn't work without this
                        throw new SecurityException("%s");
                    }
                    $_ = $proceed($$);
                    """.formatted(message);
        }
    }
}
