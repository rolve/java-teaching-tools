package ch.trick17.jtt.memcompile;

import javax.tools.DiagnosticCollector;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static java.io.Writer.nullWriter;
import static java.util.Locale.ROOT;
import static javax.tools.Diagnostic.Kind.ERROR;

public class InMemCompilation {

    public record Result(boolean errors, List<InMemClassFile> output) {}

    public static Result compile(Compiler compiler, Path sourcesDir,
                                 ClassPath classPath, PrintStream out)
            throws IOException {
        var sources = new ArrayList<InMemSource>();
        try (var javaFiles = Files.walk(sourcesDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))) {
            for (var file : (Iterable<Path>) javaFiles::iterator) {
                sources.add(InMemSource.fromFile(file));
            }
        }
        return compile(compiler, sources, classPath, out);
    }

    public static Result compile(Compiler compiler, List<InMemSource> sources,
                                 ClassPath classPath, PrintStream diagnosticsOut) throws IOException {
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
                    .filter(d -> d.getKind() == ERROR).toList();
            errors.forEach(d -> diagnosticsOut.println(d.getMessage(ROOT)));
            return new Result(!errors.isEmpty(), fileManager.getOutput());
        }
    }
}
