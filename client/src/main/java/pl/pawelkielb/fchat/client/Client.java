package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.data.Message;
import pl.pawelkielb.fchat.client.data.Name;
import pl.pawelkielb.fchat.client.packets.Packet;
import pl.pawelkielb.fchat.client.packets.RequestMessagesPacket;
import pl.pawelkielb.fchat.client.packets.SendMessagePacket;
import pl.pawelkielb.fchat.client.packets.UpdateChannelPacket;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static pl.pawelkielb.fchat.client.exceptions.Exceptions.u;

public class Client {
    private final Connection connection;
    private final Database database;

    public Client(Connection connection, Database database) {
        this.connection = connection;
        this.database = database;
    }

    public void init() {
        ClientConfig defaultClientConfig = ClientConfig.defaults();
        database.saveClientConfig(defaultClientConfig);
    }

    public void sync() throws IOException {
        connection.connect();

        Packet packet;
        do {
            packet = connection.read();
            if (packet instanceof UpdateChannelPacket updateChannelPacket) {
                ChannelConfig channelConfig = new ChannelConfig(updateChannelPacket.channelId());
                database.saveChannelConfig(updateChannelPacket.channelName(), channelConfig);
            }
        } while (packet != null);
    }

    public void createPrivateChannel(Name recipient) throws IOException {
        createGroupChannel(recipient, List.of(recipient));
    }

    public void createGroupChannel(Name name, List<Name> members) throws IOException {
        sync();

        UUID channelId = UUID.randomUUID();
        ChannelConfig channelConfig = new ChannelConfig(channelId);
        database.saveChannelConfig(name, channelConfig);

        for (var member : members) {
            UpdateChannelPacket updateChannelPacket = UpdateChannelPacket.withRandomId(
                    channelId,
                    name,
                    member
            );
            connection.send(updateChannelPacket);
        }
    }

    public void sendMessage(UUID channel, Message message) throws IOException {
        sync();

        SendMessagePacket sendMessagePacket = new SendMessagePacket(
                channel,
                message
        );

        connection.send(sendMessagePacket);
    }

    public Stream<Message> readMessages(UUID channel, int count) throws IOException {
        sync();

        RequestMessagesPacket requestMessagesPacket = new RequestMessagesPacket(channel, count);
        connection.send(requestMessagesPacket);

        return IntStream.range(0, count).mapToObj(u((i) -> {
            while (true) {
                Packet packet = connection.read();
                if (packet instanceof SendMessagePacket sendMessagePacket) {
                    return sendMessagePacket.message();
                }
            }
        }));
    }
}
