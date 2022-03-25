package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.ChannelUpdatedPacket;
import pl.pawelkielb.fchat.utils.Futures;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static pl.pawelkielb.fchat.Exceptions.r;

public class Database {
    private final Executor ioThreads;
    private final Executor workerThreads;
    private final Path updatesDirectory;
    private final Path messagesDirectory;
    private final PacketEncoder packetEncoder;

    public Database(Executor workerThreads,
                    Executor ioThreads,
                    Path rootDirectory,
                    PacketEncoder packetEncoder) {

        this.ioThreads = ioThreads;
        this.workerThreads = workerThreads;
        this.updatesDirectory = rootDirectory.resolve("updates");
        this.messagesDirectory = rootDirectory.resolve("messages");
        this.packetEncoder = packetEncoder;
    }

    private static String nameToFilename(Name name) {
        return String.valueOf(name.value().toLowerCase().hashCode());
    }

    public Observable<ChannelUpdatedPacket> listUpdatePackets(Name username) {
        Observable<ChannelUpdatedPacket> updatePackets = new Observable<>();
        Path directory = updatesDirectory.resolve(nameToFilename(username));

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

    private static void newLine(RandomAccessFile raf) throws IOException {
        raf.write("\n".getBytes());
    }

    public CompletableFuture<Void> saveMessage(UUID channel, Message message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Path directory = messagesDirectory.resolve(channel.toString());
        Path messagesPath = directory.resolve("messages.txt");
        Path indexPath = directory.resolve("index");

        ioThreads.execute(r(() -> {
            Files.createDirectories(directory);
            long start;
            long length;
            try (RandomAccessFile raf = new RandomAccessFile(messagesPath.toFile(), "rw")) {
                // go to end of file
                raf.seek(raf.length());

                start = raf.getFilePointer();
                raf.write(message.author().value().getBytes());
                newLine(raf);
                raf.write(message.content().getBytes());
                long end = raf.getFilePointer();
                length = end - start;
                newLine(raf);
                newLine(raf);
            }

            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
            buffer.putLong(start);
            buffer.putLong(length);

            Files.write(indexPath, buffer.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            future.complete(null);
        }));

        return future;
    }
}
