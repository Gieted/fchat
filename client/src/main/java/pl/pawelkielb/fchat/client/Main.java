package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.packets.SendMessagePacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

public class Main {
    public static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var clientPropertiesReader = Files.newBufferedReader(path)) {
            properties.load(clientPropertiesReader);
            return properties;
        }
    }

    public static ClientProperties readClientProperties(Path path) throws IOException {
        Properties clientProperties = readProperties(path);
        String username = clientProperties.getProperty("username");
        String serverHost = clientProperties.getProperty("server_host");
        int serverPort = Integer.parseInt(clientProperties.getProperty("server_port"));

        return new ClientProperties(Name.of(username), serverHost, serverPort);
    }

    public static ChannelProperties readChannelProperties(Path path) throws IOException {
        Properties channelProperties = readProperties(path);
        String channelId = channelProperties.getProperty("id");
        String channelName = channelProperties.getProperty("name");

        return new ChannelProperties(channelId, channelName);
    }

    public static void main(String[] args) {
        boolean isDevModeEnabled = System.getProperty("DEV_MODE") != null;
        ExceptionHandler exceptionHandler = new ExceptionHandler(isDevModeEnabled);
        PacketEncoder packetEncoder = new PacketEncoder();

        Path clientPropertiesPath = Paths.get("fchat.properties");
        Path channelPropertiesPath = Paths.get("channel.properties");
        ClientProperties clientProperties;
        ChannelProperties channelProperties = null;
        try {
            clientProperties = readClientProperties(clientPropertiesPath);
        } catch (NoSuchFileException e) {
            try {
                clientProperties = readClientProperties(Paths.get("..").resolve(clientPropertiesPath));
                channelProperties = readChannelProperties(channelPropertiesPath);
            } catch (NoSuchFileException e1) {
                exceptionHandler.onClientPropertiesNotFound();
                return;
            } catch (IOException e1) {
                e.printStackTrace();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (args.length == 0) {
            System.out.println(
                    """
                            commands:
                              fchat init
                              fchat create
                              fchat send
                              fchat read
                              fchat sync
                              fchat sendfile
                              fchat download
                              fchat live"""
            );
            System.exit(0);
        }

        String command = args[0];
        switch (command) {
            case "send" -> {
                if (args.length < 2) {
                    exceptionHandler.onMessageNotProvided();
                    return;
                }

                if (channelProperties == null) {
                    exceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                String message = String.join(" ", Arrays.asList(args).subList(1, args.length));
                SendMessagePacket sendMessagePacket = new SendMessagePacket(
                        clientProperties.username(),
                        UUID.fromString(channelProperties.id()),
                        message
                );

                Connection connection = new Connection(
                        clientProperties.serverHost(),
                        clientProperties.serverPort(),
                        packetEncoder
                );

                try {
                    connection.connect();
                    connection.send(sendMessagePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            case "read" -> {
                if (channelProperties == null) {
                    exceptionHandler.onCommandNotUsedInChannelDirectory();
                }
            }

            case "sync" -> {

            }
        }
    }
}
