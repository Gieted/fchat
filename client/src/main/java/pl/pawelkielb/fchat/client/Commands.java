package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.data.Message;
import pl.pawelkielb.fchat.client.data.Name;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileWriteException;

import java.io.IOException;
import java.util.List;

public abstract class Commands {
    private static void printMessage(Console console, Message message) {
        console.println(String.format("%s: %s", message.author(), message.content()));
        console.println();
    }

    public static void execute(String command,
                               List<String> args,
                               ClientConfig clientConfig,
                               ChannelConfig channelConfig,
                               Client client,
                               Console console) {

        if (command.equals("init")) {
            ExceptionHandler.onInitCalledInFchatDirectory();

            try {
                client.init();
            } catch (FileWriteException e) {
                ExceptionHandler.onCannotWriteFile(e.getPath());
            }

            System.exit(0);
        }

        try {
            client.sync();
        } catch (IOException e) {
            ExceptionHandler.onNetworkException();
        }

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

            }
        }
    }
}
