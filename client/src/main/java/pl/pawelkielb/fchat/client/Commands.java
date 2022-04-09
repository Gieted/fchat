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

            ClientConfig defaultClientConfig = ClientConfig.defaults();
            try {
                database.saveClientConfig(defaultClientConfig);
                return;
            } catch (FileWriteException e) {
                ExceptionHandler.onCannotWriteFile(e.getPath(), e);
                throw new AssertionError();
            }
        }

        // other commands require client config
        if (clientConfig == null) {
            ExceptionHandler.onCannotFindClientConfig();
            throw new AssertionError();
        }

        Path logsPath;
        if (channelConfig == null) {
            logsPath = Paths.get("logs.txt");
        } else {
            logsPath = Paths.get("..", "logs.txt");
        }

        Logger logger = new FileLogger(logsPath, applicationExitEvent);

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
                if (args.size() == 0) {
                    ExceptionHandler.onMissingArgument("Please provide a channel name");
                }

                Name channelName;
                try {
                    channelName = Name.of(args.get(0));
                } catch (IllegalArgumentException e) {
                    ExceptionHandler.onIllegalNameProvided(e);
                    return;
                }

                var members = args
                        .subList(1, args.size())
                        .stream()
                        .filter(Name::isValid)
                        .map(Name::of)
                        .toList();

                doNetwork(() -> client.createGroupChannel(channelName, members));
            }

            case "priv" -> {
                if (args.size() == 0) {
                    ExceptionHandler.onIllegalArgument("Please provide recipient's username");
                }

                Name recipient;
                try {
                    recipient = Name.of(args.get(0));
                } catch (IllegalArgumentException e) {
                    ExceptionHandler.onIllegalArgument("Invalid recipient", e);
                    return;
                }
                doNetwork(() -> client.createPrivateChannel(recipient));
            }

            case "send" -> {
                if (args.size() < 1) {
                    ExceptionHandler.onMissingArgument("Please provide a message");
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
                ProgressBar progressBar = new ProgressBar(console);
                doNetwork(() -> client.sendFile(channelConfig.id(), path, progressBar::update));
                console.updateLine("");
            }

            case "download" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                if (args.size() == 0) {
                    ExceptionHandler.onMissingArgument("Please provide a file name");
                }

                String fileName = args.get(0);
                ProgressBar progressBar = new ProgressBar(console);
                doNetwork(() -> client.downloadFile(channelConfig.id(), fileName, Paths.get("."), progressBar::update));
                console.updateLine("");
            }

            default -> ExceptionHandler.onUnknownCommand(command);
        }
    }

    private static void printMessage(Console console, Message message) {
        console.println(String.format("%s: %s", message.author(), message.content()));
    }

    private static void doNetwork(Exceptions.Runnable_WithExceptions<IOException> runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            ExceptionHandler.onNetworkException(e);
        } catch (DisconnectedException e) {
            ExceptionHandler.onServerDisconnected(e);
        }
    }
}
