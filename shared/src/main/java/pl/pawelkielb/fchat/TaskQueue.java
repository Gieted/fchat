package pl.pawelkielb.fchat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class TaskQueue {

    private record Task<T>(Supplier<CompletableFuture<T>> fn, CompletableFuture<T> future) {
    }

    private final Queue<Task<?>> tasks = new LinkedList<>();
    private ReentrantLock lock = new ReentrantLock();

    private <T> void processTask(Task<T> task) {
        task.fn.get().thenAccept(result -> {
            task.future.complete(result);
            runNext();
        });
    }

    private void runNext() {
        var task = tasks.poll();
        if (task != null) {
            processTask(task);
        } else {
            lock = new ReentrantLock();
        }
    }

    public <T> CompletableFuture<T> runSuspend(Supplier<CompletableFuture<T>> fn) {
        CompletableFuture<T> future = new CompletableFuture<>();
        tasks.add(new Task<>(fn, future));
        if (!lock.isLocked() && lock.tryLock()) {
            runNext();
        }

        return future;
    }

    public CompletableFuture<Void> run(Runnable fn) {
        return runSuspend(() -> {
            fn.run();
            return CompletableFuture.completedFuture(null);
        });
    }

    public <T> CompletableFuture<T> run(Supplier<T> fn) {
        return runSuspend(() -> CompletableFuture.completedFuture(fn.get()));
    }
}
