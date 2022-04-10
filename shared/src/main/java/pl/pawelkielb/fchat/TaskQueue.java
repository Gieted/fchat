package pl.pawelkielb.fchat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * A concurrency tool that allows scheduling tasks to run one-by-one. It's thread-safe.
 * A suspending task is a task that completed asynchronously.
 */
public class TaskQueue {

    private record Task<T>(Consumer<CompletableFuture<T>> fn, CompletableFuture<T> future) {
    }

    private final Queue<Task<?>> tasks = new LinkedList<>();
    private ReentrantLock lock = new ReentrantLock();

    /**
     * @return True if some task is running, false when all the tasks has been finished.
     */
    public boolean isWorking() {
        return lock.isLocked();
    }

    /**
     * Schedules a new suspending task to run.
     * <p>
     * e.g.
     * taskQueue.runSuspend(task -> {
     * doSomethingAsync().thenRun(task::complete)
     * })
     *
     * @param fn  a function to run
     * @param <T> type return by the function
     * @return A future resolving to a value returned by the function.
     */
    public <T> CompletableFuture<T> runSuspend(Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> future = new CompletableFuture<>();
        tasks.add(new Task<>(fn, future));
        if (!lock.isLocked() && lock.tryLock()) {
            runNext();
        }

        return future;
    }

    /**
     * Schedules a new no-suspending task to run.
     *
     * @param fn a function to run
     * @return A future that will be resolved when the task completes.
     */
    public CompletableFuture<Void> run(Runnable fn) {
        return runSuspend(task -> {
            fn.run();
            task.complete(null);
        });
    }

    /**
     * Schedules a new no-suspending task to run.
     *
     * @param fn  a function to run
     * @param <T> a type returned by the function
     * @return A future resolving to the value returned by the function.
     */
    public <T> CompletableFuture<T> run(Supplier<T> fn) {
        return runSuspend(task -> task.complete(fn.get()));
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
}
