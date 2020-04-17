package javagrader;

import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Results {

    private final Map<String, Set<String>> criteria = new HashMap<>();

    public synchronized void addSubmission(String name) {
        var previous = criteria.put(name, new HashSet<>());
        assert previous == null : name + " already added";
    }

    public synchronized void addCriterion(String submName, String criterion) {
        criteria.get(submName).add(criterion);
    }

    private List<String> criteriaNames() {
        return criteria.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .sorted()
                .collect(toList());
    }

    public static void write(Collection<Results> results, Path path)
            throws IOException {
        var single = results.size() == 1;
        try (var out = Files.newBufferedWriter(path)) {
            // Header
            out.append("Name");
            for (var res : results) {
                out.append(single ? "\t" : "\t\t");
                out.append(res.criteriaNames().stream().collect(joining("\t")));
            }
            out.append("\n");

            // Data
            var submNames = results.stream()
                    .flatMap(r -> r.criteria.keySet().stream())
                    .collect(toCollection(TreeSet::new));
            for (var submName : submNames) {
                out.append(submName);
                for (var res : results) {
                    var criteria = res.criteria.getOrDefault(submName, Set.of());
                    out.append(single ? "\t" : "\t\t");
                    out.append(res.criteriaNames().stream()
                            .map(c -> criteria.contains(c) ? "1" : "0")
                            .collect(joining("\t")));
                }
                out.append("\n");
            }
        }
    }
}
