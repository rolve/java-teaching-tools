import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Results {

    private final Map<String, Set<String>> criteria = new HashMap<>();

    public void addStudent(String student) {
        Object previous = criteria.put(student, new HashSet<>());
        assert previous == null : student + " already added";
    }

    public void addCriterion(String student, String criterion) {
        criteria.get(student).add(criterion);
    }

    public void writeTo(Path path) throws IOException {
        List<String> criteriaNames = criteriaNames();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.append("Name\t").append(criteriaNames.stream().collect(joining("\t"))).append("\n");

            for (String student : criteria.keySet()) {
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
