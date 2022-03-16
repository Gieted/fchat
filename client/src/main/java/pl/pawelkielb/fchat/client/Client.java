package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.config.Config;
import pl.pawelkielb.fchat.client.data.Message;
import pl.pawelkielb.fchat.client.data.Name;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.client.packets.Packet;
import pl.pawelkielb.fchat.client.packets.RequestMessagesPacket;
import pl.pawelkielb.fchat.client.packets.SendMessagePacket;
import pl.pawelkielb.fchat.client.packets.UpdateChannelPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static pl.pawelkielb.fchat.client.exceptions.Exceptions.u;

public class Client {
    private final Connection connection;

    public Client(Connection connection) {
        this.connection = connection;
    }


    public static String sanitizeAsPath(String string) {
        return string.replaceAll("[^a-zA-z ]", "");
    }

    public void init(Path directory) {
        ClientConfig defaultClientConfig = ClientConfig.defaults();
        Config.saveClientConfig(directory, defaultClientConfig);
    }

    public void createPrivateChannel(Path directory, Name recipient) throws IOException {
        createGroupChannel(directory, recipient, List.of(recipient));
    }

    public void createGroupChannel(Path directory, Name name, List<Name> members) throws IOException {
        connection.connect();

        Path path = directory.resolve(sanitizeAsPath(name.value()));
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            throw new FileWriteException(path);
        }

        ChannelConfig channelConfig = new ChannelConfig(name);
        Config.saveChannelConfig(directory, channelConfig);
        for (var member : members) {
            UpdateChannelPacket updateChannelPacket = new UpdateChannelPacket(
                    channelConfig.id(),
                    channelConfig.name(),
                    member
            );
            connection.send(updateChannelPacket);
        }
    }

    public void sendMessage(UUID channel, Message message) throws IOException {
        connection.connect();

        SendMessagePacket sendMessagePacket = new SendMessagePacket(
                channel,
                message
        );

        connection.send(sendMessagePacket);
    }

    public Stream<Message> readMessages(UUID channel, int count) throws IOException {
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
