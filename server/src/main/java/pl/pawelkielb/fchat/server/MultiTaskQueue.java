package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.TaskQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MultiTaskQueue<K> {
    private final Map<K, TaskQueue> queues = new HashMap<>();
    private final TaskQueue masterQueue = new TaskQueue();

    public <T> CompletableFuture<T> runSuspend(K key, Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> masterFuture = new CompletableFuture<>();

        masterQueue.run(() -> {
            // cleanup queues
            queues.entrySet().removeIf(entry -> entry.getKey() != key && !entry.getValue().isWorking());

            TaskQueue taskQueue = queues.get(key);
            if (taskQueue == null) {
                taskQueue = new TaskQueue();
                queues.put(key, taskQueue);
            }
            
            taskQueue.runSuspend(task -> {
                CompletableFuture<T> taskFuture = new CompletableFuture<>();
                taskFuture.thenAccept(result -> masterQueue.run(() -> {
                    task.complete(null);
                    masterFuture.complete(null);
                }));
                fn.accept(taskFuture);
            });
        });

        return masterFuture;
    }
}
