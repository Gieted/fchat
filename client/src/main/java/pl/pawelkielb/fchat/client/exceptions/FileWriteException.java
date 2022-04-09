package pl.pawelkielb.fchat.client.exceptions;

import java.io.IOException;
import java.nio.file.Path;

public class FileWriteException extends RuntimeException {
    private final Path path;

    public FileWriteException(Path path, IOException cause) {
        super(cause);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
