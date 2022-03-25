package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Event;
import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;

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
        clientConfig = database.getClientConfig();
        if (clientConfig == null) {
            Properties properties = Database.readProperties(Paths.get("channel.properties"));
            if (properties != null) {
                UUID channelId = UUID.fromString(properties.getProperty("id"));

                channelConfig = new ChannelConfig(channelId);
                database = new Database(Paths.get(".."));
                clientConfig = database.getClientConfig();
            }
        }

        PacketEncoder packetEncoder = new PacketEncoder();
        Executor executor = Runnable::run;
        Console console = new Console();
        Event applicationExitEvent = new Event();

        String command = args[0];
        List<String> commandArguments = Arrays.asList(args).subList(1, args.length);
        Commands.execute(
                command,
                commandArguments,
                clientConfig,
                channelConfig,
                console,
                database,
                packetEncoder,
                executor,
                applicationExitEvent
        );

        applicationExitEvent.trigger();
    }
}
