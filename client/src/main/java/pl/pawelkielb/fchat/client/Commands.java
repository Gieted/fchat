package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.*;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;

import java.io.IOException;
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
                logger
        );

        Client client = new Client(database, connection, clientConfig);

        switch (command) {
            case "create" -> {
                if (channelConfig != null) {
                    ExceptionHandler.onCommandUsedInChannelDirectory();
                }

                if (args.size() == 1) {
                    Name recipient = Name.of(args.get(0));
                    doNetwork(() -> client.createPrivateChannel(recipient));
                } else {
                    var members = args
                            .subList(1, args.size())
                            .stream()
                            .map(Name::of)
                            .toList();

                    doNetwork(() -> client.createGroupChannel(Name.of(args.get(0)), members));
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
                        ExceptionHandler.onIllegalArgument("messageCount must be an integer");
                    }
                }

                final int messageCountFinal = messageCount;
                doNetwork(() -> client.readMessages(channelConfig.id(), messageCountFinal)
                        .forEach(message -> printMessage(console, message)));
            }

            case "sync" -> doNetwork(client::sync);

            default -> ExceptionHandler.onUnknownCommand(command);
        }
    }
}
