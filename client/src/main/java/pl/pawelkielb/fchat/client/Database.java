package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.StringUtils;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.data.Name;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

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

    public static Properties readProperties(Path path) {
        Properties properties = new Properties();
        try (var clientPropertiesReader = Files.newBufferedReader(path)) {
            properties.load(clientPropertiesReader);
            return properties;
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new FileReadException(path);
        }
    }

    public static void writeProperties(Path path, Properties properties) {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, null);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public static ChannelConfig readChannelConfig(Path path) {
        Properties properties = readProperties(path);
        if (properties == null) {
            return null;
        }

        UUID channelId = UUID.fromString(properties.getProperty("id"));

        return new ChannelConfig(channelId);
    }

    public ClientConfig getClientConfig() {
        Path path = rootDirectory.resolve(clientConfigFileName);
        Properties properties;
        properties = readProperties(path);
        if (properties == null) {
            return null;
        }
        Name username = Name.of(properties.getProperty("username"));
        String serverHost = properties.getProperty("server_host");
        int serverPort = Integer.parseInt(properties.getProperty("server_port"));

        return new ClientConfig(username, serverHost, serverPort);
    }

    public void saveClientConfig(ClientConfig clientConfig) {
        Properties properties = new Properties();
        properties.setProperty("username", clientConfig.username().value());
        properties.setProperty("server_host", clientConfig.serverHost());
        properties.setProperty("server_port", String.valueOf(clientConfig.serverPort()));
        Path path = rootDirectory.resolve(clientConfigFileName);

        writeProperties(path, properties);
    }

    public void saveChannel(Name name, ChannelConfig channelConfig) {
        String directoryName = sanitizeAsPath(name.value());
        Path directoryPath;
        Path configPath;
        while (true) {
            directoryPath = rootDirectory.resolve(directoryName);
            configPath = directoryPath.resolve(channelConfigFileName);
            try {
                Files.createDirectory(directoryPath);
                break;
            } catch (FileAlreadyExistsException e) {
                ChannelConfig existingConfig = readChannelConfig(configPath);
                if (existingConfig != null && existingConfig.id().equals(channelConfig.id())) {
                    break;
                }
                directoryName = StringUtils.increment(directoryName);
            } catch (IOException e) {
                throw new FileWriteException(directoryPath, e);
            }
        }

        Properties properties = new Properties();
        properties.setProperty("id", channelConfig.id().toString());

        writeProperties(configPath, properties);
    }
}
