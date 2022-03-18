package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.fchat.client.exceptions.FileReadException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executor;

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
                Properties properties;
                Path path = Paths.get("channel.properties");
                properties = Database.readProperties(path);

                UUID channelId = UUID.fromString(properties.getProperty("id"));

                channelConfig = new ChannelConfig(channelId);
                database = new Database(Paths.get(".."));
                clientConfig = database.loadClientConfig();
            } catch (FileReadException e1) {
                ExceptionHandler.onCannotFindClientConfig();
                return;
            }
        }

        PacketEncoder packetEncoder = new PacketEncoder();
        Executor executorService = Runnable::run;
        Connection connection = new Connection(packetEncoder, clientConfig.serverHost(), clientConfig.serverPort(), executorService, executorService);
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
