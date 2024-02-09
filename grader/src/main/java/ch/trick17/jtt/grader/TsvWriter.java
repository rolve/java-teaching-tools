package ch.trick17.jtt.grader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Comparator.comparing;
import static java.util.EnumSet.noneOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class TsvWriter {

    public static void write(Map<Task, Map<Submission, GradeResult>> results, Path path)
            throws IOException {
        var single = results.size() == 1;
        var columnGroups = results.values().stream()
                .collect(toMap(identity(), map -> columns(map.values())));
        try (var out = newBufferedWriter(path)) {
            // Header
            out.append("Name");
            if (!single) { // two-row header
                out.append(results.entrySet().stream()
                        .map(e -> "\t\t" + e.getKey().testClassSimpleName()
                                + "\t".repeat(columnGroups.get(e.getValue()).size() - 1))
                        .collect(joining()));
                out.append("\n");
            }
            for (var r : results.values()) {
                out.append(single ? "\t" : "\t\t");
                out.append(join("\t", columnGroups.get(r)));
            }
            out.append("\n");

            // Data
            var submissions = results.values().iterator().next().keySet().stream()
                    .sorted(comparing(Submission::name))
                    .toList();
            for (var s : submissions) {
                out.append(s.name());
                for (var taskResult : results.values()) {
                    var columns = columnGroups.get(taskResult);
                    var fulfilled = fulfilledColumns(taskResult.get(s));
                    out.append(single ? "\t" : "\t\t");
                    out.append(columns.stream()
                            .map(c -> fulfilled.contains(c) ? "1" : "0")
                            .collect(joining("\t")));
                }
                out.append("\n");
            }
        }
    }

    /**
     * Determines the columns that should appear in the TSV file.
     * These include the properties and tags present for at least
     * one submission, plus all tests.
     */
    private static List<String> columns(Collection<GradeResult> results) {
        var properties = results.stream()
                .flatMap(r -> r.properties().stream())
                .collect(toCollection(() -> noneOf(Property.class))) // natural order
                .stream()
                .map(Property::prettyName);
        var tags = results.stream()
                .flatMap(r -> r.tags().stream())
                .collect(toCollection(TreeSet::new)) // alphabetical order
                .stream();
        var tests = results.stream()
                .filter(res -> !res.allTests().isEmpty())
                .findFirst().stream()
                .flatMap(res -> res.allTests().stream().map(m -> m.name())); // test execution order
        return Stream.of(properties, tags, tests)
                .flatMap(identity())
                .toList();
    }

    /**
     * Returns the columns the given result fulfills.
     */
    private static Set<String> fulfilledColumns(GradeResult results) {
        var streams = Stream.of(
                results.properties().stream().map(Property::prettyName),
                results.tags().stream(),
                results.passedTests().stream().map(m -> m.name()));
        return streams.flatMap(identity()).collect(toSet());
    }
}
