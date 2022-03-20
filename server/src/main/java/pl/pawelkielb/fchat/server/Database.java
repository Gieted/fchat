package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.ChannelUpdatedPacket;
import pl.pawelkielb.fchat.utils.Futures;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static pl.pawelkielb.fchat.Exceptions.r;

public class Database {
    private final Executor ioThreads;
    private final Executor workerThreads;
    private final Path updatesDirectory;
    private final PacketEncoder packetEncoder;

    public Database(Executor workerThreads,
                    Executor ioThreads,
                    Path rootDirectory,
                    PacketEncoder packetEncoder) {

        this.ioThreads = ioThreads;
        this.workerThreads = workerThreads;
        this.updatesDirectory = rootDirectory.resolve("updates");
        this.packetEncoder = packetEncoder;
    }

    private static String nameToFilename(Name name) {
        return String.valueOf(name.hashCode());
    }

    public Observable<ChannelUpdatedPacket> listUpdatePackets(Name username) {
        Observable<ChannelUpdatedPacket> updatePackets = new Observable<>();
        Path directory = updatesDirectory.resolve(String.valueOf(username.hashCode()));

        ioThreads.execute(r(() -> {
            if (!Files.exists(directory)) {
                updatePackets.complete();
                return;
            }

            List<CompletableFuture<Void>> futures = Files.list(directory).map(file -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                ioThreads.execute(r(() -> {
                    byte[] bytes = Files.readAllBytes(file);

                    workerThreads.execute(() -> {
                        ChannelUpdatedPacket packet = (ChannelUpdatedPacket) packetEncoder.decode(bytes);
                        updatePackets.onNext(packet);
                        future.complete(null);
                    });
                }));

                return future;
            }).toList();

            Futures.allOf(futures).thenRun(updatePackets::complete);
        }));

        return updatePackets;
    }

    public CompletableFuture<Void> saveUpdatePacket(Name username, ChannelUpdatedPacket updatedPacket) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Path directory = updatesDirectory.resolve(nameToFilename(username));
        ioThreads.execute(r(() -> {
            Files.createDirectories(directory);
            Path file = directory.resolve(updatedPacket.channel().toString());
            byte[] bytes = packetEncoder.toBytes(updatedPacket);
            Files.write(file, bytes);
            future.complete(null);
        }));

        return future;
    }

    public CompletableFuture<Void> deleteUpdatePacket(Name username, UUID channelId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Path directory = updatesDirectory.resolve(nameToFilename(username));
        ioThreads.execute(r(() -> {
            Path file = directory.resolve(channelId.toString());
            Files.delete(file);
            future.complete(null);
        }));

        return future;
    }
}
