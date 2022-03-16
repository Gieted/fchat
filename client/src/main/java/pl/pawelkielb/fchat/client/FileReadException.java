package pl.pawelkielb.fchat.client;

import java.nio.file.Path;

public class FileReadException extends RuntimeException {
    private final Path path;

    public FileReadException(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
