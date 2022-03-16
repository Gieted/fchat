package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.config.Config;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static final boolean DEV_MODE = System.getProperty("DEV_MODE") != null;

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

        ClientConfig clientConfig;
        ChannelConfig channelConfig;
        try {
            clientConfig = Config.loadClientConfig();
            channelConfig = Config.loadChannelConfig();
        } catch (FileReadException e) {
            ExceptionHandler.onCannotReadFile(e.getPath());
            return;
        }

        PacketEncoder packetEncoder = new PacketEncoder();
        Connection connection = new Connection(clientConfig.serverHost(), clientConfig.serverPort(), packetEncoder);
        Client client = new Client(connection);
        Console console = new Console();

        String command = args[0];
        List<String> commandArguments = Arrays.asList(args).subList(1, args.length);
        Commands.execute(
                command,
                commandArguments,
                clientConfig,
                channelConfig,
                client,
                console
        );
    }
}
