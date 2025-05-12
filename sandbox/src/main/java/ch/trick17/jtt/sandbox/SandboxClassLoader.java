package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassLoader;
import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.SignatureAttribute.Type;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static javassist.CtClass.booleanType;
import static javassist.CtClass.voidType;
import static javassist.Modifier.*;
import static javassist.bytecode.Opcode.GOTO;
import static javassist.bytecode.SignatureAttribute.toMethodSignature;

public class SandboxClassLoader extends InMemClassLoader {

    // use method name with special chars to avoid name clashes
    public static final String RE_INIT_METHOD = "{reInit}";

    private final long creationTime = currentTimeMillis();
    private final ClassPool pool = new ClassPool(false);
    private final Whitelist permittedCalls;
    private final boolean makeInterruptible;

    private final Set<String> sandboxedClassNames;
    // keep track of sandboxed classes (and loading order) for re-initialization
    private final Queue<Class<?>> sandboxedClasses = new ConcurrentLinkedQueue<>();

    public SandboxClassLoader(ClassPath sandboxedCode,
                              ClassPath supportCode,
                              Whitelist permittedCalls,
                              boolean makeInterruptible,
                              ClassLoader parent) throws IOException {
        super(supportCode, parent);
        this.makeInterruptible = makeInterruptible;
        try {
            var all = sandboxedCode.with(supportCode);
            for (var classFile : all.memClassPath()) {
                pool.appendClassPath(new ByteArrayClassPath(
                        classFile.getClassName(), classFile.getContent()));
            }
            for (var path : all.fileClassPath()) {
                pool.appendClassPath(path.toString());
            }
            pool.appendClassPath(new LoaderClassPath(parent));
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        this.permittedCalls = permittedCalls;

        sandboxedClassNames = new HashSet<>();
        for (var classFile : sandboxedCode.memClassPath()) {
            sandboxedClassNames.add(classFile.getClassName());
        }
        for (var path : sandboxedCode.fileClassPath()) {
            try (var walk = Files.walk(path)) {
                walk.map(Path::toString)
                        .filter(p -> p.endsWith(".class"))
                        .map(name -> name.substring(path.toString().length() + 1))
                        .map(name -> name.substring(0, name.length() - 6))
                        .map(name -> name.replace(path.getFileSystem().getSeparator(), "."))
                        .forEach(sandboxedClassNames::add);
            }
        }
    }

    public Iterable<Class<?>> getSandboxedClasses() {
        return sandboxedClasses;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!sandboxedClassNames.contains(name)) {
            return super.findClass(name);
        }

        CtClass cls;
        try {
            cls = pool.get(name);
        } catch (NotFoundException e) {
            throw new ClassNotFoundException("class not found in pool", e);
        }
        try {
            instrument(cls);
            var bytecode = cls.toBytecode();

            if (System.getProperties().containsKey("sandbox.dumpInstrumented")) {
                var file = Path.of("sandbox-dump/" + creationTime + "/" +
                        name.replace('.', '/') + ".class");
                Files.createDirectories(file.getParent());
                Files.write(file, bytecode);
            }

            var result = defineClass(name, bytecode, 0, bytecode.length);
            sandboxedClasses.add(result);
            return result;
        } catch (Exception e) {
            throw new ClassNotFoundException("could not instrument class", e);
        }
    }

    private void instrument(CtClass cls) throws Exception {
        for (var behavior : cls.getDeclaredBehaviors()) {
            if (!behavior.isEmpty()) {
                if (permittedCalls != null) {
                    behavior.instrument(new RestrictionsAdder());
                }
                if (makeInterruptible) {
                    makeInterruptible(behavior);
                }
            }
        }

        makeReInitializable(cls);
    }

    private class RestrictionsAdder extends ExprEditor {
        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            try {
                var cls = m.getClassName();
                var method = m.getMethodName();
                var sig = toMethodSignature(m.getSignature());
                var paramTypes = stream(sig.getParameterTypes())
                        .map(Type::toString)
                        .toList();
                if (!cls.endsWith("[]") && // array.clone()
                    !sandboxedClassNames.contains(cls) &&
                    !permittedCalls.methodPermitted(cls, method, paramTypes)) {
                    var params = "(" + join(",", paramTypes) + ")";
                    m.replace(createThrows(cls + "." + method + params));
                }
            } catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }

        @Override
        public void edit(NewExpr e) throws CannotCompileException {
            try {
                var cls = e.getClassName();
                var sig = toMethodSignature(e.getSignature());
                var paramTypes = stream(sig.getParameterTypes())
                        .map(Type::toString)
                        .toList();
                if (!sandboxedClassNames.contains(cls) &&
                    !permittedCalls.constructorPermitted(cls, paramTypes)) {
                    var params = "(" + join(",", paramTypes) + ")";
                    e.replace(createThrows("new " + cls + params));
                }
            } catch (BadBytecode bb) {
                throw new CannotCompileException(bb);
            }
        }

        private String createThrows(String message) {
            return """
                    if (true) { // weirdly, doesn't work without this
                        throw new SecurityException("%s");
                    }
                    $_ = $proceed($$);
                    """.formatted(message);
        }
    }

    private void makeInterruptible(CtBehavior behavior) throws Exception {
        // Inserts a check for Thread.interrupted() at the end of each loop.
        // Loops have no direct representation in bytecode, but we can use
        // the control flow graph to find basic blocks with back edges.

        // In case of multiple loops, we need to recreate the control flow graph
        // after each insertion, because the start positions and even the length
        // of the blocks may change (e.g. due to a goto becoming a goto_w).
        var processedBlocks = 0;
        while (true) { // condition can only be checked once we have the blocks
            var cfg = new ControlFlow(behavior.getDeclaringClass(), behavior.getMethodInfo());
            var backEdgeBlocks = findBlocksWithBackEdges(cfg);

            if (processedBlocks == backEdgeBlocks.size()) {
                break;
            }

            var block = backEdgeBlocks.get(processedBlocks++);
            var firstInstrIndex = block.position();
            var iterator = behavior.getMethodInfo().getCodeAttribute().iterator();
            iterator.move(firstInstrIndex);

            // find last instruction in block
            var prev = iterator.lookAhead();
            while (iterator.lookAhead() < firstInstrIndex + block.length()) {
                prev = iterator.lookAhead();
                iterator.next();
            }
            iterator.move(prev);

            insertInterruptedCheck(behavior, iterator);
        }

        // Also, need to make sure InterruptedExceptions are not swallowed
        behavior.instrument(new RethrowAdder());
    }

    private List<Block> findBlocksWithBackEdges(ControlFlow cfg) {
        var entry = cfg.basicBlocks()[0];
        var visited = new HashSet<Block>();
        var stack = new ArrayDeque<Block>();
        var result = new HashSet<Block>();
        collectBlocksWithBackEdges(entry, visited, stack, result);
        return result.stream()
                .sorted(comparing(Block::position))
                .toList();
    }

    private void collectBlocksWithBackEdges(Block block, Set<Block> visited,
                                            Deque<Block> stack, Set<Block> result) {
        visited.add(block);
        stack.push(block);
        for (int i = 0; i < block.exits(); i++) {
            var exit = block.exit(i);
            if (!visited.contains(exit)) {
                collectBlocksWithBackEdges(exit, visited, stack, result);
            } else if (stack.contains(exit)) {
                result.add(block);
            }
        }
        stack.pop();
    }

    private void insertInterruptedCheck(CtBehavior behavior, CodeIterator iterator) throws Exception {
        // Adapted from CtBehavior.insertAt(). Cannot use the simpler insertAt()
        // directly because it is based on line numbers, which cannot represent
        // the end of a loop.
        var codeAttribute = iterator.get();
        var compiler = new Javac(behavior.getDeclaringClass());
        compiler.recordParams(behavior.getParameterTypes(), isStatic(behavior.getModifiers()));
        compiler.setMaxLocals(codeAttribute.getMaxLocals());
        compiler.compileStmnt("""
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                """);
        var bytecode = compiler.getBytecode();
        codeAttribute.setMaxLocals(bytecode.getMaxLocals());
        // conservative estimate:
        codeAttribute.setMaxStack(codeAttribute.getMaxStack() + bytecode.getMaxStack());
        var insertedIndex = iterator.insert(bytecode.get());
        iterator.insert(bytecode.getExceptionTable(), insertedIndex);

        // Apparently, for empty loops (e.g. "while(true);"), which correspond
        // to a single "goto [this]" instruction, the jump offset is not
        // updated, so we do it manually.
        var nextIndex = iterator.next();
        if (iterator.byteAt(nextIndex) == GOTO && iterator.s16bitAt(nextIndex + 1) == 0) {
            iterator.write16bit(insertedIndex - nextIndex, nextIndex + 1);
        }

        behavior.getMethodInfo().rebuildStackMap(pool);
    }

    private static class RethrowAdder extends ExprEditor {
        @Override
        public void edit(Handler h) throws CannotCompileException {
            if (!h.isFinally()) {
                h.insertBefore("""
                        if ($1 instanceof InterruptedException) {
                            throw (InterruptedException) $1;
                        }
                        """);
            }
        }
    }

    /**
     * Adds a static re-initialization method to the given class, which first
     * assigns the default value to all mutable static fields and then runs a
     * copy of the class initializer code. For this to work, all static fields
     * that are final are modified to be non-final. In addition, the valueOf
     * method for enums is replaced to work around an enum constant cache.
     */
    private void makeReInitializable(CtClass cls) throws NotFoundException, CannotCompileException {
        var mutableStaticFields = stream(cls.getDeclaredFields())
                .filter(f -> isStatic(f.getModifiers()) && isMutable(f))
                .toList();
        if (mutableStaticFields.isEmpty()) {
            return;
        }

        var finalStaticFields = stream(cls.getDeclaredFields())
                .filter(f -> isStatic(f.getModifiers()) && isFinal(f.getModifiers()))
                .toList();
        if (!finalStaticFields.isEmpty()) {
            if (cls.isInterface()) {
                // fields in interfaces cannot be non-final, the JVM will check this
                throw new CannotCompileException("Cannot guarantee isolation due to mutable " +
                                                 "static fields in interface " + cls.getName());
            } else {
                for (var field : finalStaticFields) {
                    field.setModifiers(field.getModifiers() & ~FINAL);
                }
            }
        }

        var reInit = new CtMethod(voidType, RE_INIT_METHOD, new CtClass[0], cls);
        reInit.setModifiers(PUBLIC | STATIC);

        var initializer = cls.getClassInitializer();
        if (initializer != null) {
            var code = initializer.getMethodInfo().getCodeAttribute()
                    .copy(initializer.getMethodInfo().getConstPool(), null);
            reInit.getMethodInfo().setCodeAttribute((CodeAttribute) code);
        }

        var resetCode = new StringBuilder("{");
        for (var field : mutableStaticFields) {
            resetCode.append(field.getName());
            resetCode.append(field.getType().isPrimitive()
                    ? field.getType() == booleanType
                    ? " = false;"
                    : " = 0;"
                    : " = null;");
        }
        resetCode.append("}");
        if (reInit.getMethodInfo().getCodeAttribute() == null) {
            reInit.setBody(resetCode.toString());
        } else {
            reInit.insertBefore(resetCode.toString());
        }
        cls.addMethod(reInit);

        if (cls.isEnum()) {
            // valueOf uses cached enum constants in 'Class', so replace the method
            try {
                var valueOf = cls.getDeclaredMethod("valueOf");
                valueOf.setBody("""
                    {
                        Enum[] values = values();
                        for (int i = 0; i < values.length; i++) {
                            if (values[i].name().equals($1)) {
                                return values[i];
                            }
                        }
                        if ($1 == null) {
                            throw new NullPointerException("Name is null");
                        } else {
                            throw new IllegalArgumentException("No enum constant "
                                    + Enum.class.getCanonicalName() + "." + $1);
                        }
                    }
                    """.replace("Enum", cls.getName()));
            } catch (NotFoundException e) {
                // ignore; Eclipse compiler may produce broken enums without valueOf
            }
        }
    }

    private static boolean isMutable(CtField f) {
        try {
            return !isFinal(f.getModifiers()) || isMutable(f.getType());
        } catch (NotFoundException e) {
            return true;
        }
    }

    private static boolean isMutable(CtClass type) {
        // conservative...
        return !type.isPrimitive() && !type.getName().equals("java.lang.String");
    }
}
