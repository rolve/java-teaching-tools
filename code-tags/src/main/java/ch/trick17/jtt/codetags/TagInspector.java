package ch.trick17.jtt.codetags;
import static org.objectweb.asm.Opcodes.ASM7;

import java.io.PrintStream;
import java.lang.instrument.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.ProtectionDomain;

import org.objectweb.asm.*;

public class TagInspector implements ClassFileTransformer {

    private static final PrintStream STD_OUT = System.out; // System.out may be overwritten by application

    public static void premain(String args, Instrumentation inst) throws MalformedURLException {
        var codeUnderTest = Path.of(args).toUri().toURL();
        inst.addTransformer(new TagInspector(codeUnderTest));
    }

    private final URL codeUnderTest;

    public TagInspector(URL codeUnderTest) {
        this.codeUnderTest = codeUnderTest;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfile) throws IllegalClassFormatException {
        try {
            if(protectionDomain != null
                    && codeUnderTest.equals(protectionDomain.getCodeSource().getLocation())) {
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
