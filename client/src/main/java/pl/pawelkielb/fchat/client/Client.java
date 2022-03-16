package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.config.Config;
import pl.pawelkielb.fchat.client.packets.SendMessagePacket;
import pl.pawelkielb.fchat.client.packets.UpdateChannelPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class Client {
    private final Connection connection;
    private final ClientConfig clientConfig;

    public Client(Connection connection, ClientConfig clientConfig) {
        this.connection = connection;
        this.clientConfig = clientConfig;
    }

    public void init(Path directory) {
        ClientConfig defaultClientConfig = ClientConfig.defaults();
        Config.saveClientConfig(directory, defaultClientConfig);
    }

    public void createPrivateChannel(Path directory, Name recipient) {

    }

    public void createGroupChannel(Path directory, Name name, List<Name> members) throws IOException {
        connection.connect();

        try {
            Files.createDirectory(directory);
        } catch (IOException e) {
            throw new FileWriteException(directory);
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

    public void sendMessage(UUID channel, String message) throws IOException {
        connection.connect();

        SendMessagePacket sendMessagePacket = new SendMessagePacket(
                clientConfig.username(),
                channel,
                message
        );

        connection.send(sendMessagePacket);
    }
}
