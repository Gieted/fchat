package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.StringUtils;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.exceptions.DisconnectedException;
import pl.pawelkielb.fchat.exceptions.NetworkException;
import pl.pawelkielb.fchat.packets.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.file.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static pl.pawelkielb.fchat.Exceptions.throwAsUnchecked;
import static pl.pawelkielb.fchat.TransferSettings.fileChunkSizeInBytes;


/**
 * A stateful fchat client.
 */
public class Client {
    private final Database database;
    private final Connection connection;
    private final ClientConfig clientConfig;
    private boolean loggedIn = false;

    public Client(Database database, Connection connection, ClientConfig clientConfig) {
        this.database = database;
        this.connection = connection;
        this.clientConfig = clientConfig;
    }

    /**
     * Downloads updates from the server and applies them to the database.
     *
     * @throws NetworkException      if network fails
     * @throws ProtocolException     if the server does something unexpected
     * @throws DisconnectedException if the server disconnects
     * @throws FileWriteException    if saving updates fails
     */
    public void sync() throws ProtocolException {
        login();

        doSync(() -> connection.sendPacket(new RequestUpdatesPacket()));

        while (true) {
            var packet = doSync(connection::readPacket);

            if (packet == null) {
                break;
            }

            if (packet instanceof ChannelUpdatedPacket channelUpdatedPacket) {
                var channelConfig = new ChannelConfig(channelUpdatedPacket.channel());
                database.saveChannel(channelUpdatedPacket.name(), channelConfig);
            } else {
                throw new ProtocolException();
            }
        }
    }

    /**
     * @param recipient username of a user you want to chat with
     * @throws NetworkException      if network fails
     * @throws ProtocolException     if the server does something unexpected
     * @throws DisconnectedException if the server disconnects
     */
    public void createPrivateChannel(Name recipient) throws ProtocolException {
        createGroupChannel(null, List.of(recipient));
    }

    /**
     * @param name    a name of the channel. If a null is provided the name will be decided by the server.
     * @param members a list of usernames of users who should be participants of this channel
     * @throws NetworkException      if network fails
     * @throws ProtocolException     if the server does something unexpected
     * @throws DisconnectedException if the server disconnects
     */
    public void createGroupChannel(Name name, List<Name> members) throws ProtocolException {
        login();

        var channelId = UUID.randomUUID();
        var updateChannelPacket = new UpdateChannelPacket(channelId, name, members);
        doSync(() -> connection.sendPacket(updateChannelPacket));
        sync();
    }

    /**
     * @param channel an uuid of a channel you want to send the message to
     * @param message a message to send
     * @throws NetworkException      if network fails
     * @throws ProtocolException     if the server does something unexpected
     * @throws DisconnectedException if the server disconnects
     */
    public void sendMessage(UUID channel, Message message) {
        login();

        var sendMessagePacket = new SendMessagePacket(channel, message);
        doSync(() -> connection.sendPacket(sendMessagePacket));
    }

    /**
     * @param channel an uuid of a channel from which you want to read the messages
     * @param count   a count of messages to read
     * @return an iterable of read messages.
     * Calling it's next() and hasNext() might produce the same exceptions as this method call.
     * @throws NetworkException      if network fails
     * @throws ProtocolException     if the server does something unexpected
     * @throws DisconnectedException if the server disconnects
     */
    public Iterable<Message> readMessages(UUID channel, int count) {
        login();

        var requestMessagesPacket = new RequestMessagesPacket(channel, count);
        doSync(() -> connection.sendPacket(requestMessagesPacket));

        var iterator = new Iterator<Message>() {
            boolean finished = false;
            Message nextMessage = null;

            void getNext() {
                var packet = doSync(connection::readPacket);

                if (packet == null) {
                    finished = true;
                    return;
                }

                if (packet instanceof SendMessagePacket sendMessagePacket) {
                    nextMessage = sendMessagePacket.message();
                } else {
                    throwAsUnchecked(new ProtocolException());
                }
            }

            @Override
            public boolean hasNext() {
                if (nextMessage == null && !finished) {
                    getNext();
                }

                return !finished;
            }

            @Override
            public Message next() {
                if (finished) {
                    throw new NoSuchElementException();
                }

                if (nextMessage == null) {
                    getNext();
                }

                var message = nextMessage;
                nextMessage = null;

                return message;
            }
        };

        return () -> iterator;
    }

    public static class NotFileException extends RuntimeException {
    }

    /**
     * @param channel          an uuid of the channel you want to send the file to
     * @param path             a path of the file you want to send
     * @param progressConsumer a callback function, that'll be called to report the upload progress.
     *                         Its parameter is a value from 0.0 to 1.0.
     * @throws NetworkException      if network fails
     * @throws ProtocolException     if the server does something unexpected
     * @throws DisconnectedException if the server disconnects
     * @throws NoSuchFileException   if the path's target does not exist
     * @throws NotFileException      if the path's target is not a file
     * @throws FileReadException     if reading the file fails
     */
    public void sendFile(UUID channel, Path path, Consumer<Double> progressConsumer) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }

        if (!Files.isRegularFile(path)) {
            throw new NotFileException();
        }

        login();

        var totalSize = Files.size(path);
        long bytesSent = 0;

        var sendFilePacket = new SendFilePacket(channel, Name.of(path.getFileName().toString()), Files.size(path));
        doSync(() -> connection.sendPacket(sendFilePacket));

        try (InputStream inputStream = Files.newInputStream(path)) {
            while (true) {
                doSync(connection::readPacket);
                var nextBytes = inputStream.readNBytes(fileChunkSizeInBytes);
                doSync(() -> connection.sendBytes(nextBytes));
                bytesSent += nextBytes.length;
                progressConsumer.accept(((double) bytesSent) / totalSize);

                if (nextBytes.length == 0) {
                    break;
                }
            }
        }
    }

    /**
     * @param channel              an uuid of a channel from which you want to download the file
     * @param name                 a name of the file to download
     * @param destinationDirectory a directory the file will be saved to
     * @param progressConsumer     a callback function, that'll be called to report the download progress.
     *                             Its parameter is a value from 0.0 to 1.0.
     * @throws NetworkException       if network fails
     * @throws ProtocolException      if the server does something unexpected
     * @throws DisconnectedException  if the server disconnects
     * @throws NotDirectoryException  if the not a directory is passed as a directory
     * @throws NoSuchElementException if there is no file with such a name in the channel
     * @throws FileWriteException     if saving the file fails
     */
    public void downloadFile(UUID channel, Name name, Path destinationDirectory, Consumer<Double> progressConsumer)
            throws NotDirectoryException, ProtocolException {

        if (!Files.isDirectory(destinationDirectory)) {
            throw new NotDirectoryException(destinationDirectory.toString());
        }

        login();

        doSync(() -> connection.sendPacket(new RequestFilePacket(channel, name)));
        var packet = doSync(connection::readPacket);

        if (packet == null) {
            throw new NoSuchElementException(name.toString());
        }

        long fileSize;
        if (packet instanceof SendFilePacket sendFilePacket) {
            fileSize = sendFilePacket.size();
        } else {
            throw new ProtocolException();
        }

        long bytesWritten = 0;

        var filename = name.toString();
        Path filePath = null;
        OutputStream output = null;

        // find unused file name
        while (output == null) {
            filePath = destinationDirectory.resolve(filename);

            try {
                output = Files.newOutputStream(filePath, StandardOpenOption.CREATE);
            } catch (FileAlreadyExistsException e) {
                filename = StringUtils.incrementFileName(filename);
            } catch (IOException e) {
                throw new FileWriteException(filePath, e);
            }
        }

        while (true) {
            var nextBytes = doSync(connection::readBytes);
            try {
                output.write(nextBytes);
            } catch (IOException e) {
                throw new FileWriteException(filePath, e);
            }
            bytesWritten += nextBytes.length;
            progressConsumer.accept(((double) bytesWritten) / fileSize);

            if (nextBytes.length != 0) {
                doSync(() -> connection.sendPacket(null));
            } else {
                break;
            }
        }

        try {
            output.close();
        } catch (IOException e) {
            throw new FileWriteException(filePath, e);
        }
    }

    private <T> T doSync(Supplier<CompletableFuture<T>> fn) {
        try {
            return fn.get().get();
        } catch (ExecutionException e) {
            throwAsUnchecked(e.getCause());
            throw new AssertionError(e);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private void login() {
        if (!loggedIn) {
            doSync(() -> connection.sendPacket(new LoginPacket(clientConfig.username())));
            loggedIn = true;
        }
    }
}
