import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.PrintStream;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class CodeInspector implements ClassFileTransformer {

    private static final PrintStream stdOut = System.out; // System.out will be overwritten by TestRunner

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
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);
                
                collectAnnotations(classNode);
            }
            return classfile;
        } catch(Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private void collectAnnotations(ClassNode classNode) {
        if (classNode.visibleAnnotations != null) {
            classNode.visibleAnnotations.stream()
                    .filter(a -> a.desc.contains("Deductions"))
                    .flatMap(a -> a.values.stream())
                    .filter(v -> v instanceof List)
                    .flatMap(v -> ((List<?>) v).stream())
                    .map(msg -> "fix: " + msg)
                    .forEach(stdOut::println);
        }
    }
}
