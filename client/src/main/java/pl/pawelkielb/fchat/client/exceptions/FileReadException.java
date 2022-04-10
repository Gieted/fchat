package pl.pawelkielb.fchat.client.exceptions;

import java.io.IOException;
import java.nio.file.Path;

public class FileReadException extends RuntimeException {
    public FileReadException(Path path, IOException cause) {
        super(String.format("Cannot a the file (%s)", path.toAbsolutePath()), cause);
    }
}
