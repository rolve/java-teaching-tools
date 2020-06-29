package ch.trick17.jtt.grader.result;

import static java.nio.file.Files.newBufferedWriter;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeSet;

public class TsvWriter {

    public static void write(Collection<TaskResults> results, Path path)
            throws IOException {
        var single = results.size() == 1;
        var allCriteria = results.stream()
                .collect(toMap(identity(), TaskResults::allCriteria));
        try (var out = newBufferedWriter(path)) {
            // Header
            out.append("Name");
            if (!single) { // two-row header
                out.append(results.stream()
                        .map(r -> "\t\t" + r.task().testClassSimpleName()
                                + "\t".repeat(allCriteria.get(r).size() - 1))
                        .collect(joining()));
                out.append("\n");
            }
            for (var r : results) {
                out.append(single ? "\t" : "\t\t");
                out.append(allCriteria.get(r).stream().collect(joining("\t")));
            }
            out.append("\n");

            // Data
            var submNames = results.stream()
                    .flatMap(r -> r.submissionNames().stream())
                    .collect(toCollection(TreeSet::new));
            for (var submName : submNames) {
                out.append(submName);
                for (var taskResult : results) {
                    var criteria = taskResult.get(submName).criteria();
                    out.append(single ? "\t" : "\t\t");
                    out.append(allCriteria.get(taskResult).stream()
                            .map(c -> criteria.contains(c) ? "1" : "0")
                            .collect(joining("\t")));
                }
                out.append("\n");
            }
        }
    }
}
