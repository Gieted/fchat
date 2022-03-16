package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.config.Config;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
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
