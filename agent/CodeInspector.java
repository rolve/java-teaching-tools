import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Opcodes.ASM7;

import java.io.PrintStream;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Set;

import org.objectweb.asm.*;

public class CodeInspector implements ClassFileTransformer {

    private static final PrintStream STD_OUT = System.out; // System.out will be overwritten by TestRunner

    public static void premain(String rawArgs, Instrumentation inst) {
        String[] args = rawArgs.split(",");
        System.err.println("Inspecting classes " + stream(args).collect(joining(", ")));

        inst.addTransformer(new CodeInspector(stream(args).collect(toSet())));
    }

    private Set<String> classes;

    public CodeInspector(Set<String> classes) {
        this.classes = classes;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfile) throws IllegalClassFormatException {
        try {
            if(classes.contains(className)) {
                ClassReader reader = new ClassReader(classfile);
                reader.accept(new DeductionsScanner(), 0);
            }
            return classfile;
        } catch(Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private final class DeductionsScanner extends ClassVisitor {
        private DeductionsScanner() {
            super(ASM7);
        }
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!desc.contains("Deductions")) {
                return null;
            }
            return new AnnotationVisitor(ASM7) {
                public AnnotationVisitor visitArray(String name) {
                    return this;
                }
                public void visit(String name, Object value) {
                    STD_OUT.println("fix: " + value);
                }
            };
        }
    }
}
