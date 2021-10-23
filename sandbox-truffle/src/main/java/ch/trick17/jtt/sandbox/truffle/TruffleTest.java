package ch.trick17.jtt.sandbox.truffle;

import org.graalvm.polyglot.Context;

public class TruffleTest {
    public static void main(String[] args) {
        var context = Context.newBuilder()
                .allowNativeAccess(true)
                .allowCreateThread(true)
                .option("java.Classpath", System.getProperty("java.class.path"))
                .build();

        context.getBindings("java").getMember(Guest.class.getName()).invokeMember("main", (Object) null);
    }

    public static class Guest {
        public static void main(String[] args) {
            System.out.println("Hello, World!");
        }
    }
}
