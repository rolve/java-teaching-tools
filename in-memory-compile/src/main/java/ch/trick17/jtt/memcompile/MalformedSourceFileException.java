package ch.trick17.jtt.memcompile;

import java.nio.charset.MalformedInputException;
import java.nio.file.Path;

public class MalformedSourceFileException extends MalformedInputException {
    private final Path path;

    public MalformedSourceFileException(int inputLength, Path path) {
        super(inputLength);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String getMessage() {
        return path + " contains non-UTF-8 characters";
    }
}
