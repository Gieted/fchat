package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.DisconnectedException;
import pl.pawelkielb.fchat.StringUtils;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


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

    private Packet readSync() {
        try {
            return connection.readPacket().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new DisconnectedException();
        }
    }

    private void login() {
        if (!loggedIn) {
            connection.sendPacket(new LoginPacket(clientConfig.username()));
            loggedIn = true;
        }
    }

    public void sync() {
        login();

        connection.sendPacket(new RequestUpdatesPacket());

        Packet packet;
        do {
            packet = readSync();
            if (packet instanceof ChannelUpdatedPacket channelUpdatedPacket) {
                ChannelConfig channelConfig = new ChannelConfig(channelUpdatedPacket.channel());
                database.saveChannel(channelUpdatedPacket.name(), channelConfig);
            }
        } while (packet != null);
    }

    public void createPrivateChannel(Name recipient) {
        createGroupChannel(recipient, List.of(recipient));
    }

    public void createGroupChannel(Name name, List<Name> members) {
        login();

        UUID channelId = UUID.randomUUID();
        UpdateChannelPacket updateChannelPacket = new UpdateChannelPacket(channelId, name, members);
        connection.sendPacket(updateChannelPacket);
        sync();
    }

    public void sendMessage(UUID channel, Message message) {
        login();

        SendMessagePacket sendMessagePacket = new SendMessagePacket(
                channel,
                message
        );

        connection.sendPacket(sendMessagePacket);
    }

    public Stream<Message> readMessages(UUID channel, int count) {
        login();

        RequestMessagesPacket requestMessagesPacket = new RequestMessagesPacket(channel, count);
        connection.sendPacket(requestMessagesPacket);

        Iterator<Message> iterator = new Iterator<Message>() {
            boolean finished = false;
            Message nextMessage = null;

            void getNext() {
                while (true) {
                    Packet packet = readSync();

                    if (packet == null) {
                        finished = true;
                        break;
                    }

                    if (packet instanceof SendMessagePacket sendMessagePacket) {
                        nextMessage = sendMessagePacket.message();
                        break;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                if (nextMessage == null) {
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

                Message message = nextMessage;
                nextMessage = null;

                return message;
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public void sendFile(UUID channel, Path path, Consumer<Double> progressConsumer) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new NotAFileException();
        }

        login();

        long totalSize = Files.size(path);
        long bytesSent = 0;


        connection.sendPacket(new SendFilePacket(channel, path.getFileName().toString(), Files.size(path)));

        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] nextBytes;
            do {
                //noinspection StatementWithEmptyBody
                while (readSync() != null) {
                }
                nextBytes = inputStream.readNBytes(1000000);
                connection.sendBytes(nextBytes).get();
                bytesSent += nextBytes.length;
                progressConsumer.accept(((double) bytesSent) / totalSize);
            } while (nextBytes.length != 0);
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public void downloadFile(UUID channel, String name, Path destinationDirectory, Consumer<Double> progressConsumer) throws
            NotDirectoryException,
            NoSuchFileException,
            ProtocolException {

        if (!Files.isDirectory(destinationDirectory)) {
            throw new NotDirectoryException(destinationDirectory.toString());
        }

        login();

        connection.sendPacket(new RequestFilePacket(channel, name));

        Packet packet = readSync();

        if (packet == null) {
            throw new NoSuchFileException(name);
        }

        long fileSize;
        if (packet instanceof SendFilePacket sendFilePacket) {
            fileSize = sendFilePacket.size();
        } else {
            throw new ProtocolException();
        }

        long bytesWritten = 0;

        String filename = name;
        Path filePath;
        while (true) {
            filePath = destinationDirectory.resolve(filename);
            try (var output = Files.newOutputStream(filePath)) {
                byte[] nextBytes;

                do {
                    nextBytes = connection.readBytes().get();
                    output.write(nextBytes);
                    progressConsumer.accept(((double) bytesWritten) / fileSize);

                    if (nextBytes.length != 0) {
                        connection.sendPacket(null);
                    }
                } while (nextBytes.length != 0);
                break;
            } catch (FileAlreadyExistsException e) {
                filename = StringUtils.incrementFileName(filename);
            } catch (ExecutionException | InterruptedException e) {
                throw new AssertionError(e);
            } catch (IOException e) {
                throw new FileWriteException(filePath, e);
            }
        }
    }
}
