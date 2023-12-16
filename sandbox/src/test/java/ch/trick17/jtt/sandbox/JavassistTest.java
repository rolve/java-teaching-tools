package ch.trick17.jtt.sandbox;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import static java.lang.String.join;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

public class JavassistTest {

    sealed interface WhitelistEntry {
        boolean matches(MethodCall m);
    }

    record WildcardEntry(String className) implements WhitelistEntry {
        public boolean matches(MethodCall m) {
            return m.getClassName().equals(this.className);
        }
    }

    record MethodEntry(String className, String methodName) implements WhitelistEntry {
        public boolean matches(MethodCall m) {
            return m.getClassName().equals(this.className) && m.getMethodName().equals(this.methodName);
        }
    }

    public static void main(String[] args) throws Exception {
        var whitelist = """
                java.lang.Math.*
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
                for (var method : cls.getDeclaredMethods()) {
                    method.instrument(new ExprEditor() {
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (parsed.stream().noneMatch(entry -> entry.matches(m))) {
                                m.replace("""
                                        {
                                            throw new SecurityException("Illegal method call: %s");
                                            $_ = $proceed($$);
                                        }""".formatted(m.getClassName() + "." + m.getMethodName()));
                            }
                        }
                    });
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

        public static void foo() {
            var y = Math.sin(Math.PI);
            System.out.println(y);
        }

        public static void main(String[] args) {
            foo();
        }
    }
}
