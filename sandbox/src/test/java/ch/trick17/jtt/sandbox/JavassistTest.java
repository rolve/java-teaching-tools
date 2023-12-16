package ch.trick17.jtt.sandbox;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.io.FileOutputStream;
import java.io.IOException;

import static java.lang.String.join;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

public class JavassistTest {

    sealed interface WhitelistEntry {
        boolean matches(MethodCall m);
        boolean matches(NewExpr m);
    }

    record WildcardEntry(String className) implements WhitelistEntry {
        public boolean matches(MethodCall m) {
            return m.getClassName().equals(this.className);
        }
        public boolean matches(NewExpr m) {
            return m.getClassName().equals(this.className);
        }
    }

    record MethodEntry(String className, String methodName) implements WhitelistEntry {
        public boolean matches(MethodCall m) {
            return m.getClassName().equals(this.className) && m.getMethodName().equals(this.methodName);
        }
        public boolean matches(NewExpr m) {
            return m.getClassName().equals(this.className) && "<init>".equals(this.methodName);
        }
    }

    public static void main(String[] args) throws Exception {
        var whitelist = """
                java.lang.Integer.*
                java.lang.Double.*
                java.lang.String.*
                java.lang.Math.*
                java.lang.Throwable.*
                java.lang.System.currentTimeMillis
                """;

        var parsed = whitelist.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    var parts = asList(line.split("\\."));
                    var className = join(".", parts.subList(0, parts.size() - 1));
                    var methodName = parts.get(parts.size() - 1);
                    if (parts.size() > 1 && methodName.equals("*")) {
                        return new WildcardEntry(className);
                    } else {
                        return new MethodEntry(className, parts.get(parts.size() - 1));
                    }
                })
                .toList();

        var pool = ClassPool.getDefault();
        var loader = new Loader(pool);
        loader.addTranslator(pool, new Translator() {
            public void start(ClassPool pool) {}
            public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
                var cls = pool.get(classname);
                var editor = new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (parsed.stream().noneMatch(entry -> entry.matches(m))) {
                            m.replace(createThrows(m.getClassName(), m.getMethodName()));
                        }
                    }
                    public void edit(NewExpr e) throws CannotCompileException {
                        if (parsed.stream().noneMatch(entry -> entry.matches(e))) {
                            e.replace(createThrows(e.getClassName(), "<init>"));
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
                for (var constructor : cls.getDeclaredConstructors()) {
                    constructor.instrument(editor);
                }
                for (var method : cls.getDeclaredMethods()) {
                    method.instrument(editor);
                }
            }
        });
        var runner = new CustomCxtClassLoaderRunner(loader);

        runner.call(() -> {
            var cls = currentThread().getContextClassLoader().loadClass("ch.trick17.jtt.sandbox.JavassistTest$ClassUnderTest");
            return cls.getMethod("foo").invoke(null);
        });
    }

    public static class ClassUnderTest {

        public static void foo() throws IOException {
            var y = Math.sin(Math.PI);
            var bytes = Double.toString(y).getBytes();
            try (var out = new FileOutputStream("foo")) {
                out.write(bytes);
            }
        }

        public static void main(String[] args) throws IOException {
            foo();
        }
    }
}
