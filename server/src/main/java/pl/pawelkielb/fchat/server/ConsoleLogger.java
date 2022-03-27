package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Logger;
import pl.pawelkielb.fchat.TaskQueue;

import java.util.concurrent.Executor;

public class ConsoleLogger implements Logger {

    private int i = 1;
    private final TaskQueue taskQueue = new TaskQueue();
    private final Executor ioThreads;

    public ConsoleLogger(Executor ioThreads) {
        this.ioThreads = ioThreads;
    }

    @Override
    public void info(String message) {
        ioThreads.execute(() -> System.out.printf("%d: %s\n", i++, message));
    }
}
