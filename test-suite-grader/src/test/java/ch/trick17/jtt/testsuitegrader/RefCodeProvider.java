package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.InMemSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.list;

public class RefCodeProvider {

    private static final String BASE_DIR = "test-ref-code/";

    public static List<List<InMemSource>> refImplementations(String name) {
        try (var list = list(Path.of(BASE_DIR + name + "/implementations"))) {
            return list.map(f -> List.of(read(f))).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InMemSource refTestSuite(String name) {
        try (var list = list(Path.of(BASE_DIR + name + "/test-suite"))) {
            return list.map(f -> read(f)).findFirst().orElseThrow();
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
