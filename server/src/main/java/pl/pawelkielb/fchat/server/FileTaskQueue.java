package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.TaskQueue;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static pl.pawelkielb.fchat.CollectionUtils.lastOrNull;

public class FileTaskQueue<K> {
    private interface Layer {
    }

    private static class Task<T> {
        final Consumer<CompletableFuture<T>> fn;
        final CompletableFuture<T> future;
        boolean hasStarted = false;

        public Task(Consumer<CompletableFuture<T>> fn, CompletableFuture<T> future) {
            this.fn = fn;
            this.future = future;
        }
    }

    private record ReadLayer(List<Task<?>> tasks) implements Layer {
        private ReadLayer() {
            this(new ArrayList<>());
        }
    }

    private record WriteLayer(Task<?> task) implements Layer {
    }

    private final Map<K, List<Layer>> layersByKey = new HashMap<>();
    private final TaskQueue masterQueue = new TaskQueue();

    private static <T> void processLayer_processTask(Task<T> task, List<CompletableFuture<?>> futures) {
        CompletableFuture<T> future = task.future;
        futures.add(future);
        task.fn.accept(future);
    }

    private static <T> void processLayer_processTask(Task<T> task) {
        CompletableFuture<T> future = task.future;
        task.fn.accept(future);
    }

    private void processLayers(List<Layer> layers) {
        masterQueue.run(() -> {
            Layer layer = layers.get(0);
            if (layer instanceof ReadLayer readLayer) {
                List<CompletableFuture<?>> futures = new ArrayList<>();
                for (var tasksIterator = readLayer.tasks.iterator(); tasksIterator.hasNext(); ) {
                    var layerTask = tasksIterator.next();
                    if (layerTask.future.isDone()) {
                        tasksIterator.remove();
                        continue;
                    }

                    if (!layerTask.hasStarted) {
                        layerTask.hasStarted = true;
                        processLayer_processTask(layerTask, futures);
                    }
                }

                if (readLayer.tasks.isEmpty()) {
                    layers.remove(0);
                    return;
                }

                Futures.allOf(futures).thenRun(() -> processLayers(layers));
            } else if (layer instanceof WriteLayer writeLayer) {
                writeLayer.task.future.thenRun(() -> masterQueue.run(() -> layers.remove(0)));
                writeLayer.task.hasStarted = true;
                processLayer_processTask(writeLayer.task);

            } else {
                throw new AssertionError();
            }
        });
    }

    private List<Layer> getLayers(K key) {
        // cleanup layer lists
        layersByKey.entrySet().removeIf(entry -> entry.getKey() != key && entry.getValue().isEmpty());

        return layersByKey.computeIfAbsent(key, k -> new LinkedList<>());
    }

    public <T> CompletableFuture<T> runSuspendReading(K key, Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> masterFuture = new CompletableFuture<>();

        masterQueue.run(() -> {
            List<Layer> layers = getLayers(key);

            Layer lastLayer = lastOrNull(layers);
            if (lastLayer == null || lastLayer instanceof WriteLayer) {
                lastLayer = new ReadLayer();
                layers.add(lastLayer);
            }

            ((ReadLayer) lastLayer).tasks.add(new Task<>(fn, masterFuture));

            return layers;
        }).thenAccept(this::processLayers);

        return masterFuture;
    }

    public <T> CompletableFuture<T> runSuspendWriting(K key, Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> masterFuture = new CompletableFuture<>();

        masterQueue.run(() -> {
            List<Layer> layers = getLayers(key);

            WriteLayer writeLayer = new WriteLayer(new Task<>(fn, masterFuture));
            layers.add(writeLayer);

            return layers;
        }).thenAccept(this::processLayers);

        return masterFuture;
    }

    public CompletableFuture<Void> runReading(K key, Runnable fn) {
        return runSuspendReading(key, task -> {
            fn.run();
            task.complete(null);
        });
    }

    public CompletableFuture<Void> runWriting(K key, Runnable fn) {
        return runSuspendWriting(key, task -> {
            fn.run();
            task.complete(null);
        });
    }
}
