package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.packets.NotifyPacket;
import pl.pawelkielb.fchat.client.packets.SendMessagePacket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
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

        return new ChannelProperties(UUID.fromString(channelId), Name.of(channelName));
    }


    public static String sanitizeAsPath(String string) {
        return string.replaceAll("[^a-zA-z ]", "");
    }

    public static void main(String[] args) {
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
                              fchat live
                              fchat add"""
            );
            System.exit(0);
        }

        boolean isDevModeEnabled = System.getProperty("DEV_MODE") != null;
        ExceptionHandler exceptionHandler = new ExceptionHandler(isDevModeEnabled);
        PacketEncoder packetEncoder = new PacketEncoder();

        Path clientPropertiesPath = Paths.get("fchat.properties");
        Path channelPropertiesPath = Paths.get("channel.properties");

        ClientProperties clientProperties = null;
        ChannelProperties channelProperties = null;
        try {
            clientProperties = readClientProperties(clientPropertiesPath);
        } catch (NoSuchFileException e) {
            try {
                clientProperties = readClientProperties(Paths.get("..").resolve(clientPropertiesPath));
                channelProperties = readChannelProperties(channelPropertiesPath);
            } catch (NoSuchFileException ignore) {
            } catch (IOException e1) {
                e.printStackTrace();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String command = args[0];

        if (command.equals("init")) {
            if (clientProperties != null) {
                exceptionHandler.onInitCalledInFchatDirectory();
            }

            ClientProperties defaultClientProperties = ClientProperties.defaults();
            Properties properties = new Properties();
            properties.setProperty("username", defaultClientProperties.username().value());
            properties.setProperty("server_host", defaultClientProperties.serverHost());
            properties.setProperty("server_port", String.valueOf(defaultClientProperties.serverPort()));
            try {
                writeProperties(clientPropertiesPath, properties);
            } catch (IOException e) {
                exceptionHandler.onCannotSaveClientProperties();
            }
            System.exit(0);
        } else {
            if (clientProperties == null) {
                exceptionHandler.onClientPropertiesNotFound();
                return;
            }
        }

        // client properties cannot be null at this point

        Connection connection = new Connection(
                clientProperties.serverHost(),
                clientProperties.serverPort(),
                packetEncoder
        );

        switch (command) {
            case "create" -> {
                if (channelProperties != null) {
                    exceptionHandler.onCommandUsedInChannelDirectory();
                }

                String directoryName;
                Set<Name> members;
                Name channelName;

                if (args.length == 2) {
                    Name recipient = Name.of(args[1]);
                    directoryName = sanitizeAsPath(recipient.value());
                    members = new HashSet<>();
                    members.add(recipient);
                    channelName = null;
                } else {
                    channelName = Name.of(args[1]);
                    directoryName = sanitizeAsPath(channelName.value());
                    var memberNames = Arrays.asList(args)
                            .subList(2, args.length)
                            .stream()
                            .map(Name::of)
                            .toList();

                    members = new HashSet<>(memberNames);
                }

                Path directory = Paths.get(".", directoryName);
                try {
                    Files.createDirectory(directory);
                } catch (IOException e) {
                    exceptionHandler.onCannotWriteFile(directory);
                }

                ChannelProperties createdChannelProperties = new ChannelProperties(channelName);
                Properties properties = new Properties();
                properties.setProperty("id", createdChannelProperties.id().toString());
                properties.setProperty("name", createdChannelProperties.name().value());
                Path createdChannelPropertiesPath = directory.resolve(channelPropertiesPath);
                try {
                    writeProperties(createdChannelPropertiesPath, properties);
                } catch (IOException e) {
                    exceptionHandler.onCannotWriteFile(createdChannelPropertiesPath);
                }

                for (var member : members) {
                    NotifyPacket notifyPacket = new NotifyPacket(
                            createdChannelProperties.id(),
                            createdChannelProperties.name(),
                            member
                    );
                    try {
                        connection.send(notifyPacket);
                    } catch (IOException e) {
                        exceptionHandler.onNetworkException();
                    }
                }
            }

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
                        channelProperties.id(),
                        message
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
