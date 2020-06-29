package ch.trick17.jtt.codetags;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Opcodes.ASM7;

import java.io.PrintStream;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Set;

import org.objectweb.asm.*;

public class TagInspector implements ClassFileTransformer {

    private static final PrintStream STD_OUT = System.out; // System.out may be overwritten by application

    public static void premain(String rawArgs, Instrumentation inst) {
        String[] args = rawArgs.split(",");
        System.err.println("Inspecting classes " + stream(args).collect(joining(", ")));

        inst.addTransformer(new TagInspector(stream(args).collect(toSet())));
    }

    private final Set<String> classes;

    public TagInspector(Set<String> classes) {
        this.classes = classes;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfile) throws IllegalClassFormatException {
        try {
            if(classes.contains(className.replace('/', '.'))) {
                ClassReader reader = new ClassReader(classfile);
                reader.accept(new TagScanner(), 0);
            }
            return classfile;
        } catch(Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private final class TagScanner extends ClassVisitor {

        private TagScanner() {
            super(ASM7);
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!desc.replace('/', '.').contains(Tag.class.getName())) {
                return null;
            }
            return new AnnotationVisitor(ASM7) {
                public AnnotationVisitor visitArray(String name) {
                    return this;
                }
                public void visit(String name, Object value) {
                    STD_OUT.println("tag: " + value);
                }
            };
        }
    }
}
