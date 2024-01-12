package ch.trick17.jtt.memcompile;

import java.util.function.Supplier;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

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
