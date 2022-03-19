package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.UpdateChannelPacket;

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

    public Observable<UpdateChannelPacket> listUpdatePackets(Name username) {
        Observable<UpdateChannelPacket> updatePackets = new Observable<>();
        Path path = updatesDirectory.resolve(String.valueOf(username.hashCode()));

        ioThreads.execute(r(() -> {
            List<CompletableFuture<Void>> futures = Files.list(path).map(file -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                ioThreads.execute(r(() -> {
                    byte[] bytes = Files.readAllBytes(file);

                    workerThreads.execute(() -> {
                        UpdateChannelPacket packet = (UpdateChannelPacket) packetEncoder.decode(bytes);
                        updatePackets.onNext(packet);
                        future.complete(null);
                    });
                }));

                return future;
            }).toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(updatePackets::complete);
        }));

        return updatePackets;
    }

    public CompletableFuture<Void> deleteUpdatePacket(Name username, UUID packetId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Path path = updatesDirectory.resolve(String.valueOf(username.hashCode()));
        ioThreads.execute(r(() -> {
            Files.delete(path.resolve(packetId.toString()));
            future.complete(null);
        }));

        return future;
    }
}
