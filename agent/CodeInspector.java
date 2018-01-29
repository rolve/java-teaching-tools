import static java.util.stream.Collectors.toSet;

import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class CodeInspector implements ClassFileTransformer {

    private static final PrintStream stdOut = System.out; // System.out will be overwritten by TestRunner

    public static void premain(String agentArgs, Instrumentation inst) {
        String[] split = agentArgs.split(",");
        int instrThreshold = Integer.parseInt(split[0]);
        Set<String> classes = Stream.of(split).skip(1).collect(toSet());
        System.err.println("Inspecting classes " + classes);

        inst.addTransformer(new CodeInspector(classes, instrThreshold));
    }

    private Set<String> classes;
    private int instrThreshold;

    private List<MethodNode> methods = new ArrayList<>();

    public CodeInspector(Set<String> classes, int instrThreshold) {
        this.classes = classes;
        this.instrThreshold = instrThreshold;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfile) throws IllegalClassFormatException {
        if(classes.contains(className)) {
            ClassReader reader = new ClassReader(classfile);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            collectMethods(classNode);
            checkThreshold();
        }
        return classfile;
    }

    private void collectMethods(ClassNode classNode) {
        methods.addAll(classNode.methods);
    }

    private void checkThreshold() {
        int instrs = methods.stream().mapToInt(m -> m.instructions.size()).sum();
        if(instrs >= instrThreshold) {
            stdOut.println("code size above threshold");
        } else {
            System.err.println("code size threshold (" + instrThreshold + ") not (yet) reached: " + instrs);
        }
    }
}
