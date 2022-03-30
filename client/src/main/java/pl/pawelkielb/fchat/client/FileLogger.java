package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Logger;
import pl.pawelkielb.fchat.Observable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static pl.pawelkielb.fchat.Exceptions.c;

public class FileLogger implements Logger {
    private Path path;
    private final Observable<Void> applicationExitEvent;
    private BufferedWriter writer;

    public FileLogger(Path path, Observable<Void> applicationExitEvent) {
        this.path = path;
        this.applicationExitEvent = applicationExitEvent;
    }

    @Override
    public void info(String message) {
        if (writer == null) {
            try {
                writer = Files.newBufferedWriter(path);
                applicationExitEvent.subscribe(c(() -> writer.close()));
            } catch (IOException ignore) {
            }
            path = null;
        }

        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException ignore) {
        }
    }
}
