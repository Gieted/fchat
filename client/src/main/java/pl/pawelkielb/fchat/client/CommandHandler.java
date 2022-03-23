package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.client.client.Client;
import pl.pawelkielb.fchat.client.client.exceptions.AlreadyInitializedException;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

public class CommandHandler {
    private static void printMessage(Console console, Message message) {
        console.println(String.format("%s: %s", message.author(), message.content()));
        console.println();
    }

    private final ClientConfig clientConfig;
    private final ChannelConfig channelConfig;
    private final Console console;
    private final Database database;
    private final PacketEncoder packetEncoder;
    private final Executor executor;

    public CommandHandler(ClientConfig clientConfig,
                          ChannelConfig channelConfig,
                          Console console,
                          Database database,
                          PacketEncoder packetEncoder,
                          Executor executor) {

        this.clientConfig = clientConfig;
        this.channelConfig = channelConfig;
        this.console = console;
        this.database = database;
        this.packetEncoder = packetEncoder;
        this.executor = executor;
    }

    public void execute(String command, List<String> args) {
        if (command.equals("init")) {
            try {
                ClientConfig defaultClientConfig = ClientConfig.defaults();
                database.saveClientConfig(defaultClientConfig);
            } catch (FileWriteException e) {
                ExceptionHandler.onCannotWriteFile(e.getPath());
            } catch (AlreadyInitializedException e) {
                ExceptionHandler.onAlreadyInitialized();
            }

            System.exit(0);
        }

        Connection connection = new Connection(
                packetEncoder,
                clientConfig.serverHost(),
                clientConfig.serverPort(),
                executor,
                executor
        );

        Client client = new Client(database, connection, clientConfig);

        switch (command) {
            case "create" -> {
                if (channelConfig != null) {
                    ExceptionHandler.onCommandUsedInChannelDirectory();
                }

                if (args.size() == 1) {
                    Name recipient = Name.of(args.get(0));
                    try {
                        client.createPrivateChannel(recipient);
                    } catch (IOException e) {
                        ExceptionHandler.onNetworkException();
                    }
                } else {
                    var members = args
                            .subList(1, args.size())
                            .stream()
                            .map(Name::of)
                            .toList();

                    try {
                        client.createGroupChannel(Name.of(args.get(0)), members);
                    } catch (IOException e) {
                        ExceptionHandler.onNetworkException();
                    }
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

                String message = String.join(" ", args.subList(1, args.size()));
                try {
                    client.sendMessage(channelConfig.id(), new Message(clientConfig.username(), message));
                } catch (IOException e) {
                    ExceptionHandler.onNetworkException();
                }
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

                try {
                    client.readMessages(channelConfig.id(), messageCount).forEach(message -> printMessage(console, message));
                } catch (IOException e) {
                    ExceptionHandler.onNetworkException();
                }
            }

            case "sync" -> {
                try {
                    client.sync();
                } catch (IOException e) {
                    ExceptionHandler.onNetworkException();
                }
            }
        }
    }
}
