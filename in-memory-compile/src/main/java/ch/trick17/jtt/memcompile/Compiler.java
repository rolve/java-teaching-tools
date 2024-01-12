package ch.trick17.jtt.memcompile;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.function.Supplier;

public enum Compiler {

    JAVAC(ToolProvider::getSystemJavaCompiler),
    ECLIPSE(EclipseCompiler::new);

    private final Supplier<JavaCompiler> supplier;

    Compiler(Supplier<JavaCompiler> sup) {
        this.supplier = sup;
    }

    public JavaCompiler create() {
        return supplier.get();
    }
}
