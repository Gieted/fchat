package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Logger;
import pl.pawelkielb.fchat.TaskQueue;

public class ConsoleLogger implements Logger {

    private int i = 1;
    private final TaskQueue taskQueue = new TaskQueue();

    @Override
    public void info(String message) {
        taskQueue.run(() -> System.out.printf("%d: %s\n", i++, message));
    }
}
