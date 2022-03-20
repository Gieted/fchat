package pl.pawelkielb.fchat.client.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.client.ConnectionFactory;
import pl.pawelkielb.fchat.client.Database;
import pl.pawelkielb.fchat.client.client.exceptions.AlreadyInitializedException;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static pl.pawelkielb.fchat.Exceptions.i;

public class Client {
    private Connection connection;
    private final Database database;
    private final ConnectionFactory connectionFactory;

    public Client(Database database, ConnectionFactory connectionFactory) {
        this.database = database;
        this.connectionFactory = connectionFactory;
    }

    public void init() {
        if (database.getClientConfig() != null) {
            throw new AlreadyInitializedException();
        }
        ClientConfig defaultClientConfig = ClientConfig.defaults();
        database.saveClientConfig(defaultClientConfig);
    }

    private void connect() {
        if (connection == null) {
            ClientConfig clientConfig = database.getClientConfig();
            connection = connectionFactory.create(clientConfig);
            connection.send(new LoginPacket(clientConfig.username()));
        }
    }

    private Packet readSync() {
        try {
            return connection.read().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionError();
        }
    }

    public void sync() throws IOException {
        connect();

        Packet packet;
        do {
            packet = readSync();
            if (packet instanceof ChannelUpdatedPacket channelUpdatedPacket) {
                ChannelConfig channelConfig = new ChannelConfig(channelUpdatedPacket.channel());
                database.saveChannelConfig(channelUpdatedPacket.name(), channelConfig);
            }
        } while (packet != null);

        System.out.println("sync finished!");
    }

    public void createPrivateChannel(Name recipient) throws IOException {
        connect();

        createGroupChannel(recipient, List.of(recipient));
    }

    public void createGroupChannel(Name name, List<Name> members) throws IOException {
        connect();

        UUID channelId = UUID.randomUUID();
        UpdateChannelPacket updateChannelPacket = new UpdateChannelPacket(channelId, name, members);
        connection.send(updateChannelPacket);
        sync();
    }

    public void sendMessage(UUID channel, Message message) throws IOException {
        connect();

        SendMessagePacket sendMessagePacket = new SendMessagePacket(
                channel,
                message
        );

        connection.send(sendMessagePacket);
    }

    public Stream<Message> readMessages(UUID channel, int count) throws IOException {
        connect();

        RequestMessagesPacket requestMessagesPacket = new RequestMessagesPacket(channel, count);
        connection.send(requestMessagesPacket);

        return IntStream.range(0, count).mapToObj(i((i) -> {
            while (true) {
                Packet packet = connection.read().get();
                if (packet instanceof SendMessagePacket sendMessagePacket) {
                    return sendMessagePacket.message();
                }
            }
        }));
    }
}
