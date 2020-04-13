package javagrader;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Results {

    private final Map<String, Set<String>> criteria = new HashMap<>();

    public synchronized void addStudent(String student) {
        Object previous = criteria.put(student, new HashSet<>());
        assert previous == null : student + " already added";
    }

    public synchronized void addCriterion(String student, String criterion) {
        criteria.get(student).add(criterion);
    }

    public synchronized void writeTo(Path path) throws IOException {
        List<String> criteriaNames = criteriaNames();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.append("NETHZ\t").append(criteriaNames.stream().collect(joining("\t"))).append("\n");

            for (String student : criteria.keySet().stream().sorted().toArray(String[]::new)) {
                Set<String> presentCriteria = criteria.get(student);
                writer.append(student).append("\t");
                writer.append(criteriaNames.stream().map(c -> presentCriteria.contains(c) ? "1" : "0").collect(joining("\t")));
                writer.append("\n");
            }
        }
    }

    private List<String> criteriaNames() {
        return criteria.values().stream().flatMap(Set::stream).distinct().sorted().collect(toList());
    }
}
