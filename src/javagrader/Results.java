package javagrader;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Results {

    private final Map<String, Set<String>> criteria = new TreeMap<>();

    public synchronized void addStudent(String student) {
        var previous = criteria.put(student, new HashSet<>());
        assert previous == null : student + " already added";
    }

    public synchronized void addCriterion(String student, String criterion) {
        criteria.get(student).add(criterion);
    }

    public synchronized void writeTo(Path path) throws IOException {
        var crit = criteriaNames();

        try (var writer = Files.newBufferedWriter(path)) {
            writer.append("NETHZ\t");
            writer.append(crit.stream().collect(joining("\t"))).append("\n");

            for (var student : criteria.keySet()) {
                var presentCriteria = criteria.get(student);
                writer.append(student).append("\t");
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
