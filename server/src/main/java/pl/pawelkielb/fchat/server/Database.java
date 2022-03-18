package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.UpdateChannelPacket;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

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

    public CompletableFuture<Stream<CompletableFuture<UpdateChannelPacket>>> listUpdatePackets(Name username) {
        Path path = updatesDirectory.resolve(String.valueOf(username.hashCode()));
        CompletableFuture<Stream<CompletableFuture<UpdateChannelPacket>>> streamFuture = new CompletableFuture<>();

        ioThreads.execute(r(() -> {
            Stream<CompletableFuture<UpdateChannelPacket>> stream = Files.list(path).map(file -> {
                CompletableFuture<UpdateChannelPacket> future = new CompletableFuture<>();
                ioThreads.execute(r(() -> {
                    byte[] bytes = Files.readAllBytes(file);
                    workerThreads.execute(() -> {
                        UpdateChannelPacket packet = (UpdateChannelPacket) packetEncoder.decode(bytes);
                        future.complete(packet);
                    });
                }));

                return future;
            });
            streamFuture.complete(stream);
        }));

        return streamFuture;
    }

    public CompletableFuture<?> deleteUpdatePacket(Name username, UUID packetId) {
        CompletableFuture<?> future = new CompletableFuture<>();
        Path path = updatesDirectory.resolve(String.valueOf(username.hashCode()));
        ioThreads.execute(r(() -> {
            Files.delete(path.resolve(packetId.toString()));
            future.complete(null);
        }));

        return future;
    }
}
