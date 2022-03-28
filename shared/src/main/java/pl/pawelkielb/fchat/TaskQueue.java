package pl.pawelkielb.fchat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class TaskQueue {

    private record Task<T>(Consumer<CompletableFuture<T>> fn, CompletableFuture<T> future) {
    }

    private final Queue<Task<?>> tasks = new LinkedList<>();
    private ReentrantLock lock = new ReentrantLock();

    public boolean isWorking() {
        return lock.isLocked();
    }

    private <T> void processTask(Task<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.thenAccept(result -> {
            task.future.complete(result);
            runNext();
        });
        task.fn.accept(future);
    }

    private void runNext() {
        var task = tasks.poll();
        if (task != null) {
            processTask(task);
        } else {
            lock = new ReentrantLock();
        }
    }

    public <T> CompletableFuture<T> runSuspend(Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> future = new CompletableFuture<>();
        tasks.add(new Task<>(fn, future));
        if (!lock.isLocked() && lock.tryLock()) {
            runNext();
        }

        return future;
    }

    public CompletableFuture<Void> run(Runnable fn) {
        return runSuspend(task -> {
            fn.run();
            task.complete(null);
        });
    }
}
