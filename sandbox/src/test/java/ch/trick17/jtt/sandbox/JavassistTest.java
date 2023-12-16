package ch.trick17.jtt.sandbox;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class JavassistTest {
    public static void main(String[] args) throws Exception {
        var pool = ClassPool.getDefault();
        var cls = pool.get("ch.trick17.jtt.sandbox.ClassUnderTest");
        var foo = cls.getDeclaredMethod("foo");
        foo.instrument(new ExprEditor() {
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
        cls.toClass(JavassistTest.class);

        ClassUnderTest.foo();
    }
}
