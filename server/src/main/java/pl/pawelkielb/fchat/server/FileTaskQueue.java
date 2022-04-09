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

    private record FileData(TaskQueue taskQueue, List<Layer> layers) {
        private FileData() {
            this(new TaskQueue(), new LinkedList<>());
        }
    }

    private final Map<K, FileData> queues = new HashMap<>();
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

    private void processLayer(CompletableFuture<Void> processLayerTask, FileData fileData) {
        masterQueue.run(() -> {
            Layer layer = fileData.layers.get(0);
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
                    fileData.layers.remove(0);
                    processLayerTask.complete(null);
                    return;
                }

                Futures.allOf(futures).thenRun(() -> processLayer(processLayerTask, fileData));
            } else if (layer instanceof WriteLayer writeLayer) {
                writeLayer.task.future.thenRun(() -> masterQueue.run(() -> {
                    fileData.layers.remove(0);
                    processLayerTask.complete(null);
                }));
                writeLayer.task.hasStarted = true;
                processLayer_processTask(writeLayer.task);

            } else {
                throw new AssertionError();
            }
        });
    }

    private FileData getFileData(K key) {
        // cleanup queues
        queues.entrySet().removeIf(entry -> entry.getKey() != key && !entry.getValue().taskQueue.isWorking());

        FileData fileData = queues.get(key);
        if (fileData == null) {
            fileData = new FileData();
            queues.put(key, fileData);
        }

        return fileData;
    }

    public <T> CompletableFuture<T> runSuspendReading(K key, Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> masterFuture = new CompletableFuture<>();

        masterQueue.run(() -> {
            FileData fileData = getFileData(key);

            Layer lastLayer = lastOrNull(fileData.layers);
            if (lastLayer == null || lastLayer instanceof WriteLayer) {
                lastLayer = new ReadLayer();
                fileData.layers.add(lastLayer);
            }

            ((ReadLayer) lastLayer).tasks.add(new Task<>(fn, masterFuture));
            fileData.taskQueue.<Void>runSuspend(task -> processLayer(task, fileData));
        });

        return masterFuture;
    }

    public <T> CompletableFuture<T> runSuspendWriting(K key, Consumer<CompletableFuture<T>> fn) {
        CompletableFuture<T> masterFuture = new CompletableFuture<>();

        masterQueue.run(() -> {
            FileData fileData = getFileData(key);

            WriteLayer writeLayer = new WriteLayer(new Task<>(fn, masterFuture));
            fileData.layers.add(writeLayer);
            fileData.taskQueue.<Void>runSuspend(task -> processLayer(task, fileData));
        });

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
