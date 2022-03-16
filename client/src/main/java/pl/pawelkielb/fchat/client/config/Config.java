package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.client.data.Name;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

public abstract class Config {
    public static final String clientConfigFileName = "fchat.properties";
    public static final String channelConfigFileName = "channel.properties";

    public static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var clientPropertiesReader = Files.newBufferedReader(path)) {
            properties.load(clientPropertiesReader);
            return properties;
        }
    }

    public static void writeProperties(Path path, Properties properties) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, null);
        }
    }

    private static ClientConfig readClientConfig(Path path) throws IOException {
        Properties properties = readProperties(path);
        String username = properties.getProperty("username");
        String serverHost = properties.getProperty("server_host");
        int serverPort = Integer.parseInt(properties.getProperty("server_port"));

        return new ClientConfig(Name.of(username), serverHost, serverPort);
    }

    public static ClientConfig loadClientConfig() {
        ClientConfig clientConfig;
        Path path = Paths.get(clientConfigFileName);
        try {
            clientConfig = readClientConfig(path);
        } catch (NoSuchFileException e1) {
            try {
                path = Paths.get("..", clientConfigFileName);
                clientConfig = readClientConfig(path);
            } catch (NoSuchFileException e2) {
                clientConfig = null;
            } catch (IOException e) {
                throw new FileReadException(path);
            }
        } catch (IOException e) {
            throw new FileReadException(path);
        }

        return clientConfig;
    }

    public static ChannelConfig loadChannelConfig() {
        Properties properties;
        Path path = Paths.get(channelConfigFileName);
        try {
            properties = readProperties(path);
        } catch (IOException e) {
            throw new FileReadException(path);
        }
        String channelId = properties.getProperty("id");
        String channelName = properties.getProperty("name");

        return new ChannelConfig(UUID.fromString(channelId), Name.of(channelName));
    }

    public static void saveClientConfig(Path directory, ClientConfig clientConfig) {
        Properties properties = new Properties();
        properties.setProperty("username", clientConfig.username().value());
        properties.setProperty("server_host", clientConfig.serverHost());
        properties.setProperty("server_port", String.valueOf(clientConfig.serverPort()));
        Path path = directory.resolve(clientConfigFileName);
        try {
            writeProperties(path, properties);
        } catch (IOException e) {
            throw new FileWriteException(path);
        }
    }

    public static void saveChannelConfig(Path directory, ChannelConfig channelConfig) {
        Properties properties = new Properties();
        properties.setProperty("id", channelConfig.id().toString());
        Path path = directory.resolve(channelConfigFileName);
        try {
            writeProperties(path, properties);
        } catch (IOException e) {
            throw new FileWriteException(path);
        }
    }
}
