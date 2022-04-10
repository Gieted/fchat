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

import static pl.pawelkielb.fchat.Functions.c;
import static pl.pawelkielb.fchat.Functions.r;


/**
 * Allows saving and loading the server's data. It's thread-safe.
 */
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

    /**
     * Saves a {@link ChannelUpdatedPacket} for the given username.
     *
     * @param username
     * @param channelUpdatedPacket
     * @return A future that will be resolved when the saving completes.
     */
    public CompletableFuture<Void> saveChannelUpdatedPacket(Name username, ChannelUpdatedPacket channelUpdatedPacket) {
        Path directory = updatesDirectory.resolve(nameToFilename(username));

        return updatesTaskQueue.runSuspendWriting(username, task -> ioThreads.execute(r(() -> {
            Files.createDirectories(directory);
            Path file = directory.resolve(channelUpdatedPacket.channel().toString());
            byte[] bytes = packetEncoder.toBytes(channelUpdatedPacket);
            Files.write(file, bytes);

            logger.info(String.format("Saved update for user %s: %s", username, channelUpdatedPacket));
            task.complete(null);
        })));
    }

    /**
     * Reads all channel updated packets for the given username.
     *
     * @param username
     * @return An observable of the packets.
     * Will complete instantly if there are no channel updated packets for the given username.
     */
    public Observable<ChannelUpdatedPacket> listChannelUpdatedPackets(Name username) {
        Observable<ChannelUpdatedPacket> updatePackets = new Observable<>();
        Path directory = updatesDirectory.resolve(nameToFilename(username));

        updatesTaskQueue.runSuspendReading(username, task -> ioThreads.execute(r(() -> {
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

    /**
     * Deletes a {@link ChannelUpdatedPacket} for the given username.
     *
     * @param username
     * @param channelId an uuid of the channel of the ChannelUpdatedPacket
     * @return A future that will be resolved when the deletion completes.
     */
    public CompletableFuture<Void> deleteChannelUpdatedPacket(Name username, UUID channelId) {
        return updatesTaskQueue.runSuspendWriting(username, task -> {
            Path directory = updatesDirectory.resolve(nameToFilename(username));
            ioThreads.execute(r(() -> {
                Path file = directory.resolve(channelId.toString());
                Files.delete(file);
                task.complete(null);
            }));
        });
    }

    /**
     * @param channel a channel on which the message should be saved
     * @param message
     * @return A future that will be resolved when the saving completes.
     */
    public CompletableFuture<Void> saveMessage(UUID channel, Message message) {
        return messagesTaskQueue.runSuspendWriting(channel, task -> {
            Path directory = messagesDirectory.resolve(channel.toString());
            Path messagesPath = directory.resolve("messages.txt");
            Path indexPath = directory.resolve("index");

            ioThreads.execute(r(() -> {
                Files.createDirectories(directory);
                long start;
                long length;
                try (RandomAccessFile raf = new RandomAccessFile(messagesPath.toFile(), "rw")) {
                    // go to end of the file
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

    /**
     * @param channel the channel from which to read the messages
     * @param count   a count of messages to read
     * @return An Observable of the messages.
     */
    public Observable<Message> getMessages(UUID channel, int count) {
        Observable<Message> messages = new Observable<>();

        messagesTaskQueue.runSuspendReading(channel, task -> {
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

    /**
     * @param channel         a channel on which the file has been sent
     * @param nameProposition a proposition of a name. If it's already taken the database will choose the new one.
     * @param file            an AsyncStream of the file bytes. The database will send a byte count as a request
     *                        and the async stream should publish the requested count of bytes to the stream.
     * @return a future resolving to a name, under which the file was saved
     */
    public CompletableFuture<String> saveFile(UUID channel, String nameProposition, AsyncStream<Integer, byte[]> file) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Path filesDirectory = messagesDirectory.resolve(channel.toString()).resolve("files");

        fileCreationTaskQueue.<Void>runSuspendWriting(channel, fileCreationTask -> ioThreads.execute(r(() -> {
            Files.createDirectories(filesDirectory);

            String fileName = nameProposition;
            while (true) {
                Path filePath = filesDirectory.resolve(fileName);
                try {
                    var output = Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                    final String fileNameFinal = fileName;

                    fileTaskQueue.runSuspendWriting(filePath, task -> file.subscribe(1000, c(nextBytes -> {
                        output.write(nextBytes);
                        file.requestNext(1000);
                    }), r(() -> {
                        logger.info("Saved file: " + fileNameFinal);
                        output.close();
                        task.complete(null);
                        future.complete(fileNameFinal);
                    }), future::completeExceptionally));

                    fileCreationTask.complete(null);
                    break;
                } catch (FileAlreadyExistsException e) {
                    fileName = StringUtils.incrementFileName(fileName);
                }
            }
        })));

        return future;
    }

    /**
     * @param channel a channel from which to read the file
     * @param name    a name of the file
     * @return An async stream of the file.
     * Consumer must send byte count as a request and the database will respond will the requested count of bytes.
     */
    public AsyncStream<Integer, byte[]> getFile(UUID channel, String name) {
        Observable<Integer> producer = new Observable<>();
        Observable<byte[]> consumer = new Observable<>();
        Path path = messagesDirectory.resolve(channel.toString()).resolve("files").resolve(name);

        fileCreationTaskQueue.runReading(channel, () ->
                fileTaskQueue.runSuspendReading(path, c(fileTask -> {
                    try {
                        var inputStream = Files.newInputStream(path);
                        producer.subscribe(byteCount -> {
                            try {
                                byte[] nextBytes = inputStream.readNBytes(byteCount);
                                if (nextBytes.length == 0) {
                                    producer.complete();
                                    consumer.complete();
                                } else {
                                    consumer.onNext(nextBytes);
                                }
                            } catch (IOException e) {
                                fileTask.complete(null);
                                consumer.onException(e);
                            }
                        }, () -> fileTask.complete(null), e -> {
                            fileTask.complete(null);
                            consumer.onException(e);
                        });
                    } catch (IOException e) {
                        fileTask.complete(null);
                        consumer.onException(e);
                    }
                })));

        return new AsyncStream<>(producer, consumer);
    }

    /**
     * @param channel a channel on which the file has been sent
     * @param name    a name of the file
     * @return A future resolving to the file size
     */
    public CompletableFuture<Long> getFileSize(UUID channel, String name) {
        CompletableFuture<Long> result = new CompletableFuture<>();
        Path path = messagesDirectory.resolve(channel.toString()).resolve("files").resolve(name);

        fileCreationTaskQueue.runReading(channel, () ->
                fileTaskQueue.runSuspendReading(path, task -> ioThreads.execute(() -> {
                    try {
                        long size = Files.size(path);
                        task.complete(null);
                        result.complete(size);
                    } catch (IOException e) {
                        task.complete(null);
                        result.completeExceptionally(e);
                    }
                })));

        return result;
    }

    private static void newLine(RandomAccessFile raf) throws IOException {
        raf.write("\n".getBytes());
    }

    private final FileTaskQueue<UUID> messagesTaskQueue = new FileTaskQueue<>();

    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        return buffer.getLong(0);
    }

    private final FileTaskQueue<UUID> fileCreationTaskQueue = new FileTaskQueue<>();
    private final FileTaskQueue<Path> fileTaskQueue = new FileTaskQueue<>();


    private static String nameToFilename(Name name) {
        return String.valueOf(name.value().toLowerCase().hashCode());
    }

    private final FileTaskQueue<Name> updatesTaskQueue = new FileTaskQueue<>();
}
