package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;

import java.nio.file.Paths;
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
                              fchat live"""
            );
            System.exit(0);
        }

        Database database = new Database(Paths.get("."));

        ClientConfig clientConfig;
        ChannelConfig channelConfig = null;
        try {
            clientConfig = database.loadClientConfig();
        } catch (FileReadException e) {
            try {
                channelConfig = ChannelConfig.load();
                database = new Database(Paths.get(".."));
                clientConfig = database.loadClientConfig();
            } catch (FileReadException e1) {
                ExceptionHandler.onCannotFindClientConfig();
                return;
            }
        }

        PacketEncoder packetEncoder = new PacketEncoder();
        Connection connection = new Connection(clientConfig.serverHost(), clientConfig.serverPort(), packetEncoder);
        Client client = new Client(connection, database);
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
