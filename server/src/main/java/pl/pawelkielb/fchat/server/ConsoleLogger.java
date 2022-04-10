package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Logger;
import pl.pawelkielb.fchat.TaskQueue;

import java.util.concurrent.Executor;


/**
 * A {@link Logger} that logs to a console. It's thread-safe.
 */
public class ConsoleLogger implements Logger {
    private long i = 1;
    private final TaskQueue taskQueue = new TaskQueue();
    private final Executor ioThreads;

    public ConsoleLogger(Executor ioThreads) {
        this.ioThreads = ioThreads;
    }

    @Override
    public void info(String message) {
        taskQueue.runSuspend(task -> ioThreads.execute(() -> {
            System.out.printf("%d: %s\n", i++, message);
            task.complete(null);
        }));
    }
}
