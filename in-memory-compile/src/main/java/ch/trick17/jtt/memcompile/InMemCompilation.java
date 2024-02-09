package ch.trick17.jtt.memcompile;

import javax.tools.DiagnosticCollector;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static java.io.Writer.nullWriter;
import static java.util.Locale.ROOT;
import static javax.tools.Diagnostic.Kind.ERROR;

public class InMemCompilation {

    public record Result(boolean errors, List<InMemClassFile> output) {}

    public static Result compile(Compiler compiler,
                                 List<InMemSource> sources,
                                 ClassPath classPath,
                                 PrintStream out) throws IOException {
        try (var fileManager = new InMemFileManager(sources, classPath)) {
            var javaCompiler = compiler.create();
            var collector = new DiagnosticCollector<>();
            var version = Integer.toString(Runtime.version().feature());
            var options = new ArrayList<>(List.of(
                    "-source", version, "-target", version));
            if (compiler == ECLIPSE) {
                options.add("-proceedOnError");
            }
            javaCompiler.getTask(nullWriter(), fileManager, collector,
                    options, null, sources).call();

            var errors = collector.getDiagnostics().stream()
                    .filter(d -> d.getKind() == ERROR)
                    .map(d -> d.getMessage(ROOT))
                    .distinct()
                    .toList();
            if (!errors.isEmpty()) {
                out.println("Compile errors:");
                errors.forEach(e -> out.print(e.indent(2))); // indent includes \n
            }
            return new Result(!errors.isEmpty(), fileManager.getOutput());
        }
    }
}
