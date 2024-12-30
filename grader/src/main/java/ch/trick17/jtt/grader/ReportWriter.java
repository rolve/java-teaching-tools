package ch.trick17.jtt.grader;

import ch.trick17.jtt.grader.BatchGrader.Submission;
import ch.trick17.jtt.grader.Grader.Result;
import ch.trick17.jtt.grader.Grader.Task;
import ch.trick17.jtt.testrunner.ExceptionDescription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
                var report = new StringBuilder();
                for (var task : results.keySet()) {
                    var result = format(results.get(task).get(subm));
                    if (!result.isEmpty()) {
                        report.append(indent(1)).append(join(", ", task.testClassNames())).append("\n");
                        report.append(result);
                    }
                }
                if (!report.isEmpty()) {
                    out.append(subm.name()).append('\n');
                    out.append(report);
                    out.append('\n');
                }
            }
        }
    }

    private static CharSequence format(Result result) {
        var formatted = new StringBuilder();
        if (!result.compileErrors().isEmpty()) {
            formatted.append(indent(2)).append("Compile errors:").append('\n');
            for (var error : result.compileErrors()) {
                formatted.append(indent(3)).append(error).append('\n');
            }
        }
        if (!result.testCompileErrors().isEmpty()) {
            formatted.append(indent(2)).append("Compile errors in tests:").append('\n');
            for (var error : result.testCompileErrors()) {
                formatted.append(indent(3)).append(error).append('\n');
            }
        }
        if (!result.failedTests().isEmpty()) {
            formatted.append(indent(2)).append("Failed tests:").append('\n');
            for (var testResult : result.testResults()) {
                if (!testResult.passed()) {
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
                    formatted.append(indent(3)).append(testResult.method().toString())
                            .append(": ").append(join(", ", properties)).append('\n');
                    for (var exception : testResult.exceptions()) {
                        formatted.append(indent(INDENT_SIZE)).append(exception.className()).append(": ")
                                .append(formatMsg(exception)).append('\n');
                    }
                }
            }
        }
        return formatted;
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
