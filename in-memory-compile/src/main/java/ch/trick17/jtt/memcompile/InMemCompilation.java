package ch.trick17.jtt.memcompile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static java.io.Writer.nullWriter;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.NOPOS;

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
        try (var fileManager = new InMemFileManager(classPath)) {
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

            errors.forEach(d -> diagnosticsOut.println(format(d)));
            return new Result(!errors.isEmpty(), fileManager.getOutput());
        }
    }

    private static String format(Diagnostic<?> problem) {
        var path = ((InMemSource) problem.getSource()).toUri().getPath();
        return path
               + ":" + problem.getLineNumber()
               + ": " + problem.getKind()
               + ": " + problem.getMessage(null) + "\n"
               + formatSource(problem);
    }

    /**
     * Compiler-independent formatting of source location, based on
     * {@link
     * org.eclipse.jdt.internal.compiler.problem.DefaultProblem#errorReportSource(char[])}
     */
    private static CharSequence formatSource(Diagnostic<?> problem) {
        char[] unitSource = ((InMemSource) problem.getSource())
                .getCharContent(true).toCharArray();

        var startPos = (int) problem.getStartPosition();
        var endPos = (int) problem.getEndPosition();
        int len;
        if (startPos > endPos
            || startPos == NOPOS && endPos == NOPOS
            || (len = unitSource.length) == 0) {
            return "";
        }

        char c;
        int start;
        int end;
        for (start = startPos >= len ? len - 1 : startPos; start > 0; start--) {
            if ((c = unitSource[start - 1]) == '\n' || c == '\r') {
                break;
            }
        }
        for (end = endPos >= len ? len - 1 : endPos; end + 1 < len; end++) {
            if ((c = unitSource[end + 1]) == '\r' || c == '\n') {
                break;
            }
        }

        // trim left and right spaces/tabs
        while ((c = unitSource[start]) == ' ' || c == '\t') {
            start++;
        }
        while ((c = unitSource[end]) == ' ' || c == '\t') {
            end--;
        }

        // copy source
        var result = new StringBuffer();
        result.append('\t').append(unitSource, start, end - start + 1);
        result.append("\n\t");

        // compute underline
        for (int i = start; i < startPos; i++) {
            result.append((unitSource[i] == '\t') ? '\t' : ' ');
        }
        result.append("^".repeat(Math.max(0, (endPos >= len ? len - 1 : endPos) - startPos + 1)));
        return result;
    }
}
