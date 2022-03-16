package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.client.Database;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

public record ChannelConfig(
        UUID id
) {
    public static ChannelConfig load() {
        Properties properties;
        Path path = Paths.get(Database.channelConfigFileName);
        try {
            properties = Database.readProperties(path);
        } catch (IOException e) {
            throw new FileReadException(path);
        }

        UUID channelId = UUID.fromString(properties.getProperty("id"));

        return new ChannelConfig(channelId);
    }
}
