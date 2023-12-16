package ch.trick17.jtt.sandbox;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import static java.lang.Thread.currentThread;

public class JavassistTest {
    public static void main(String[] args) throws Exception {
        var pool = ClassPool.getDefault();
        var loader = new Loader(pool);
        loader.addTranslator(pool, new Translator() {
            public void start(ClassPool pool) {}
            public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
                var cls = pool.get(classname);
                for (var method : cls.getDeclaredMethods()) {
                    method.instrument(new ExprEditor() {
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (m.getClassName().equals("java.lang.Math") &&
                                m.getMethodName().equals("sin")) {
                                m.replace("""
                                        {
                                            System.out.println("sin");
                                            $_ = $proceed($$);
                                        }""");
                            }
                        }
                    });
                }
            }
        });
        var runner = new CustomCxtClassLoaderRunner(loader);

        runner.call(() -> {
            var cls = currentThread().getContextClassLoader().loadClass("ch.trick17.jtt.sandbox.ClassUnderTest");
            return cls.getMethod("foo").invoke(null);
        });
    }
}
