package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.*;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class Commands {
    private static void printMessage(Console console, Message message) {
        console.println(String.format("%s: %s", message.author(), message.content()));
    }

    private static void doNetwork(Exceptions.Runnable_WithExceptions<IOException> runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            ExceptionHandler.onNetworkException();
        } catch (DisconnectedException e) {
            ExceptionHandler.onServerDisconnected();
        }
    }

    public static void execute(String command,
                               List<String> args,
                               ClientConfig clientConfig,
                               ChannelConfig channelConfig,
                               Console console,
                               Database database,
                               PacketEncoder packetEncoder,
                               Executor executor,
                               Observable<Void> applicationExitEvent) {

        if (command.equals("init")) {
            if (clientConfig != null) {
                ExceptionHandler.onAlreadyInitialized();
            }

            try {
                ClientConfig defaultClientConfig = ClientConfig.defaults();
                database.saveClientConfig(defaultClientConfig);
            } catch (FileWriteException e) {
                ExceptionHandler.onCannotWriteFile(e.getPath());
            }

            return;
        }

        if (clientConfig == null) {
            ExceptionHandler.onCannotFindClientConfig();
            return;
        }

        Path rootPath;
        if (channelConfig == null) {
            rootPath = Paths.get("logs.txt");
        } else {
            rootPath = Paths.get("..", "logs.txt");
        }

        Logger logger = new FileLogger(rootPath, applicationExitEvent);
        Connection connection = new Connection(
                packetEncoder,
                clientConfig.serverHost(),
                clientConfig.serverPort(),
                executor,
                executor,
                logger,
                applicationExitEvent
        );

        Client client = new Client(database, connection, clientConfig);

        switch (command) {
            case "create" -> {
                if (args.size() == 1) {
                    Name recipient = Name.of(args.get(0));
                    doNetwork(() -> client.createPrivateChannel(recipient));
                } else {
                    var members = args
                            .subList(1, args.size())
                            .stream()
                            .filter(Name::isValid)
                            .map(Name::of)
                            .toList();

                    Name channelName;
                    try {
                        channelName = Name.of(args.get(0));
                    } catch (IllegalArgumentException e) {
                        ExceptionHandler.onIllegalNameProvided();
                        return;
                    }
                    doNetwork(() -> client.createGroupChannel(channelName, members));
                }
            }

            case "send" -> {
                if (args.size() < 1) {
                    ExceptionHandler.onMessageNotProvided();
                    return;
                }

                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                String message = String.join(" ", args);
                doNetwork(() -> client.sendMessage(channelConfig.id(), new Message(clientConfig.username(), message)));
            }

            case "read" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                int messageCount = 100;
                if (args.size() > 0) {
                    try {
                        messageCount = Integer.parseInt(args.get(0));
                    } catch (NumberFormatException e) {
                        ExceptionHandler.onIllegalArgument("The first argument has to be an integer");
                    }
                }

                if (messageCount < 1) {
                    return;
                }

                final int messageCountFinal = messageCount;
                doNetwork(() -> client.readMessages(channelConfig.id(), messageCountFinal)
                        .forEach(message -> printMessage(console, message)));
            }

            case "sync" -> doNetwork(client::sync);

            case "sendfile" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                Path path = Paths.get(args.get(0));
                doNetwork(() -> client.sendFile(channelConfig.id(), path, System.out::println));
            }

            default -> ExceptionHandler.onUnknownCommand(command);

            case "download" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                String fileName = args.get(0);
                doNetwork(() -> client.downloadFile(channelConfig.id(), fileName, Paths.get("."), System.out::println));
            }
        }
    }
}
