package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RefCodeProvider {

    private static final String BASE_DIR = "test-ref-code/";

    public static List<List<InMemSource>> refImplementations(String name) {
        var list = list(Path.of(BASE_DIR + name + "/implementations"));
        if (list.stream().allMatch(Files::isDirectory)) {
            return list.stream()
                    .map(dir -> list(dir).stream().map(f -> read(f)).toList())
                    .toList();
        } else if (list.stream().allMatch(Files::isRegularFile)) {
            return list.stream().map(f -> List.of(read(f))).toList();
        } else {
            throw new IllegalArgumentException("All files or all directories expected");
        }
    }

    public static InMemSource refTestSuite(String name) {
        return list(Path.of(BASE_DIR + name + "/test-suite")).stream()
                .map(f -> read(f))
                .findFirst().orElseThrow();
    }

    private static List<Path> list(Path dir) {
        try (var list = Files.list(dir)) {
            return list.toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InMemSource read(Path file) {
        try {
            return InMemSource.fromString(Files.readString(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
