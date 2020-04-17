package javagrader;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Results {

    private final Map<String, Set<String>> criteria = new TreeMap<>();

    public synchronized void addSubmission(String name) {
        var previous = criteria.put(name, new HashSet<>());
        assert previous == null : name + " already added";
    }

    public synchronized void addCriterion(String submName, String criterion) {
        criteria.get(submName).add(criterion);
    }

    public void writeTo(Path path) throws IOException {
        var crit = criteriaNames();

        try (var writer = Files.newBufferedWriter(path)) {
            writer.append("NETHZ\t");
            writer.append(crit.stream().collect(joining("\t"))).append("\n");

            for (var submName : criteria.keySet()) {
                var presentCriteria = criteria.get(submName);
                writer.append(submName).append("\t");
                writer.append(crit.stream()
                        .map(c -> presentCriteria.contains(c) ? "1" : "0")
                        .collect(joining("\t")));
                writer.append("\n");
            }
        }
    }

    private List<String> criteriaNames() {
        return criteria.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .sorted()
                .collect(toList());
    }
}
