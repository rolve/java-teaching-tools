package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.BatchGrader.Submission;
import ch.trick17.jtt.grader.Grader.Result;
import ch.trick17.jtt.grader.Grader.Task;
import ch.trick17.jtt.testrunner.ExceptionDescription;
import ch.trick17.jtt.testrunner.TestResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static java.lang.String.valueOf;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Comparator.comparing;

public class ReportWriter {

    private static final int INDENT_SIZE = 4;

    public static void write(Map<Task, Map<Submission, Result>> results,
                             Path file) throws IOException {
        var submissions = results.values().iterator().next().keySet().stream()
                .sorted(comparing(Submission::name))
                .toList();
        try (var out = newBufferedWriter(file)) {
            for (var subm : submissions) {
                out.append(subm.name()).append('\n');
                for (var task : results.keySet()) {
                    var classes = join(", ", task.testClassNames());
                    out.append(indent(1)).append(classes).append('\n');
                    format(results.get(task).get(subm), 2, out);
                }
                out.append('\n');
            }
        }
    }

    public static void format(Result result, int indent,
                              Appendable out) throws IOException {
        if (!result.compileErrors().isEmpty()) {
            out.append(indent(indent)).append("Compile errors:").append('\n');
            for (var error : result.compileErrors()) {
                out.append(indent(indent + 1)).append(error).append('\n');
            }
        }
        if (!result.testCompileErrors().isEmpty()) {
            out.append(indent(indent)).append("Compile errors in tests:").append('\n');
            for (var error : result.testCompileErrors()) {
                out.append(indent(indent + 1)).append(error).append('\n');
            }
        }
        formatTestResults(result.testResults(), indent, out);
    }

    public static void formatTestResults(List<TestResult> testResults, int indent,
                                         Appendable out) throws IOException {
        for (var testResult : testResults) {
            var properties = new ArrayList<String>();
            if (testResult.nonDeterm()) {
                properties.add("non-deterministic");
            }
            if (testResult.incompleteReps()) {
                properties.add("incomplete repetitions (" + testResult.repsMade() + ")");
            }
            if (testResult.timeout()) {
                properties.add("timeout");
            }
            if (testResult.outOfMemory()) {
                properties.add("out of memory");
            }
            if (!testResult.illegalOps().isEmpty()) {
                properties.add("illegal operations (" + join(", ", testResult.illegalOps()) + ")");
            }
            out.append(indent(indent)).append(testResult.passed() ? '✅' : '❌')
                    .append(' ').append(testResult.method().toString());
            if (!testResult.passed()) {
                out.append(": ").append(join(", ", properties));
            }
            out.append('\n');
            for (var exception : testResult.exceptions()) {
                out.append(indent(indent + 1)).append(exception.className()).append(": ")
                        .append(formatMsg(exception)).append('\n');
            }
        }
    }

    private static String formatMsg(ExceptionDescription exception) {
        if (exception.className().equals("java.lang.Error") &&
            exception.message().startsWith("Unresolved compilation problem")) {
            // don't repeat compilation error details
            return "Unresolved compilation problem";
        } else {
            // indent all lines except the first
            var indented = valueOf(exception.message()).indent(5 * INDENT_SIZE);
            return indented.substring(5 * INDENT_SIZE, indented.length() - 1);
        }
    }

    private static String indent(int depth) {
        return " ".repeat(depth * INDENT_SIZE);
    }
}
