package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.data.Name;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Database {
    private final Path rootDirectory;

    public Database(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public static final String clientConfigFileName = "fchat.properties";
    public static final String channelConfigFileName = "channel.properties";

    public static String sanitizeAsPath(String string) {
        return string.replaceAll("[^0-9a-zA-z ]", "");
    }

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

    public ClientConfig loadClientConfig() {
        Path path = rootDirectory.resolve(clientConfigFileName);
        try {
            Properties properties = readProperties(path);
            Name username = Name.of(properties.getProperty("username"));
            String serverHost = properties.getProperty("server_host");
            int serverPort = Integer.parseInt(properties.getProperty("server_port"));

            return new ClientConfig(username, serverHost, serverPort);
        } catch (IOException e) {
            throw new FileReadException(path);
        }
    }

    public void saveClientConfig(ClientConfig clientConfig) {
        Properties properties = new Properties();
        properties.setProperty("username", clientConfig.username().value());
        properties.setProperty("server_host", clientConfig.serverHost());
        properties.setProperty("server_port", String.valueOf(clientConfig.serverPort()));
        Path path = rootDirectory.resolve(clientConfigFileName);
        try {
            writeProperties(path, properties);
        } catch (IOException e) {
            throw new FileWriteException(path);
        }
    }

    public void saveChannelConfig(Name name, ChannelConfig channelConfig) {
        Path directoryPath = rootDirectory.resolve(sanitizeAsPath(name.value()));
        try {
            Files.createDirectory(directoryPath);
        } catch (IOException e) {
            throw new FileWriteException(directoryPath);
        }

        Properties properties = new Properties();
        properties.setProperty("id", channelConfig.id().toString());
        Path configPath = directoryPath.resolve(channelConfigFileName);
        try {
            writeProperties(configPath, properties);
        } catch (IOException e) {
            throw new FileWriteException(configPath);
        }
    }
}