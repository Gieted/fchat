package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Logger;
import pl.pawelkielb.fchat.Observable;
import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.StringUtils;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.ChannelUpdatedPacket;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static pl.pawelkielb.fchat.Exceptions.c;
import static pl.pawelkielb.fchat.Exceptions.r;

public class Database {
    private final Executor ioThreads;
    private final Executor workerThreads;
    private final Path updatesDirectory;
    private final Path messagesDirectory;
    private final PacketEncoder packetEncoder;
    private final Logger logger;

    public Database(Executor workerThreads,
                    Executor ioThreads,
                    Path rootDirectory,
                    PacketEncoder packetEncoder, Logger logger) {

        this.ioThreads = ioThreads;
        this.workerThreads = workerThreads;
        this.updatesDirectory = rootDirectory.resolve("updates");
        this.messagesDirectory = rootDirectory.resolve("messages");
        this.packetEncoder = packetEncoder;
        this.logger = logger;
    }

    private static String nameToFilename(Name name) {
        return String.valueOf(name.value().toLowerCase().hashCode());
    }

    private final MultiTaskQueue<Name> updatesTaskQueue = new MultiTaskQueue<>();

    public Observable<ChannelUpdatedPacket> listUpdatePackets(Name username) {
        Observable<ChannelUpdatedPacket> updatePackets = new Observable<>();
        Path directory = updatesDirectory.resolve(nameToFilename(username));

        updatesTaskQueue.runSuspend(username, task -> ioThreads.execute(r(() -> {
            if (!Files.exists(directory)) {
                task.complete(null);
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

            Futures.allOf(futures).thenRun(() -> {
                logger.info(String.format("Read %d updates for user %s", futures.size(), username));
                task.complete(null);
                updatePackets.complete();
            });
        })));

        return updatePackets;
    }

    public CompletableFuture<Void> saveUpdatePacket(Name username, ChannelUpdatedPacket updatedPacket) {
        Path directory = updatesDirectory.resolve(nameToFilename(username));

        return updatesTaskQueue.runSuspend(username, task -> ioThreads.execute(r(() -> {
            Files.createDirectories(directory);
            Path file = directory.resolve(updatedPacket.channel().toString());
            byte[] bytes = packetEncoder.toBytes(updatedPacket);
            Files.write(file, bytes);

            logger.info(String.format("Saved update for user %s: %s", username, updatedPacket));
            task.complete(null);
        })));
    }

    public CompletableFuture<Void> deleteUpdatePacket(Name username, UUID channelId) {
        return updatesTaskQueue.runSuspend(username, task -> {
            Path directory = updatesDirectory.resolve(nameToFilename(username));
            ioThreads.execute(r(() -> {
                Path file = directory.resolve(channelId.toString());
                Files.delete(file);
                task.complete(null);
            }));
        });
    }

    private static void newLine(RandomAccessFile raf) throws IOException {
        raf.write("\n".getBytes());
    }

    private final MultiTaskQueue<UUID> messagesTaskQueue = new MultiTaskQueue<>();

    public CompletableFuture<Void> saveMessage(UUID channel, Message message) {
        return messagesTaskQueue.runSuspend(channel, task -> {
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

                logger.info(String.format("Saved message for channel %s: %s", channel, message));
                task.complete(null);
            }));
        });
    }

    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        return buffer.getLong(0);
    }

    public Observable<Message> getMessages(UUID channel, int count) {
        Observable<Message> messages = new Observable<>();

        messagesTaskQueue.runSuspend(channel, task -> {
            Path directory = messagesDirectory.resolve(channel.toString());
            Path messagesPath = directory.resolve("messages.txt");
            Path indexPath = directory.resolve("index");

            ioThreads.execute(r(() -> {
                long messagesRead = 0;
                try (RandomAccessFile index = new RandomAccessFile(indexPath.toFile(), "r");
                     RandomAccessFile messagesFile = new RandomAccessFile(messagesPath.toFile(), "r")) {

                    long startPosition = index.length() - (Long.BYTES * 2L * count);
                    startPosition = startPosition > 0 ? startPosition : 0;
                    index.seek(startPosition);
                    while (index.getFilePointer() < index.length()) {
                        byte[] startBytes = new byte[Long.BYTES];
                        byte[] lengthBytes = new byte[Long.BYTES];
                        index.read(startBytes);
                        index.read(lengthBytes);
                        long start = bytesToLong(startBytes);
                        int length = (int) bytesToLong(lengthBytes);

                        messagesFile.seek(start);
                        byte[] messageEntry = new byte[length];
                        messagesFile.read(messageEntry);

                        String messageString = new String(messageEntry);
                        String[] messageSplit = messageString.split("\n");
                        Name author = Name.of(messageSplit[0]);
                        String content = messageSplit[1];

                        Message message = new Message(author, content);
                        messages.onNext(message);
                        messagesRead++;
                    }
                } catch (FileNotFoundException ignore) {
                }
                logger.info(String.format("Read %d messages for channel %s", messagesRead, channel));
                task.complete(null);
                messages.complete();
            }));
        });

        return messages;
    }

    private final MultiTaskQueue<UUID> filesTaskQueue = new MultiTaskQueue<>();

    public CompletableFuture<Void> saveFile(UUID channel, String name, Observable<byte[]> bytes) {
        Path filesDirectory = messagesDirectory.resolve(channel.toString()).resolve("files");

        return filesTaskQueue.runSuspend(channel, task -> ioThreads.execute(r(() -> {
            Files.createDirectories(filesDirectory);
            String fileName = name;
            while (true) {
                Path filePath = filesDirectory.resolve(fileName);
                try {
                    var output = Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    bytes.subscribe(c(output::write), r(() -> {
                        output.close();
                        task.complete(null);
                    }));
                    break;
                } catch (FileAlreadyExistsException e) {
                    fileName = StringUtils.incrementFileName(fileName);
                }
            }
        })));
    }
}
