package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.DisconnectedException;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
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
            return connection.read().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new DisconnectedException();
        }
    }

    private void login() {
        if (!loggedIn) {
            connection.send(new LoginPacket(clientConfig.username()));
            loggedIn = true;
        }
    }

    public void sync() {
        login();

        connection.send(new RequestUpdatesPacket());

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
        connection.send(updateChannelPacket);
        sync();
    }

    public void sendMessage(UUID channel, Message message) {
        login();

        SendMessagePacket sendMessagePacket = new SendMessagePacket(
                channel,
                message
        );

        connection.send(sendMessagePacket);
    }

    public Stream<Message> readMessages(UUID channel, int count) {
        login();

        RequestMessagesPacket requestMessagesPacket = new RequestMessagesPacket(channel, count);
        connection.send(requestMessagesPacket);

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
}
