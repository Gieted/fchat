package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Observable;
import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.client.config.ChannelConfig;
import pl.pawelkielb.fchat.client.config.ClientConfig;
import pl.pawelkielb.fchat.client.exceptions.ExceptionHandler;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;


public class Main {
    public static final boolean DEV_MODE = System.getProperty("DEV_MODE") != null;

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println(
                        """
                                commands:
                                  fchat init
                                  fchat create
                                  fchat priv
                                  fchat send
                                  fchat read
                                  fchat sync
                                  fchat sendfile
                                  fchat download"""
                );
                System.exit(0);
            }

            Database database = new Database(Paths.get("."));
            ChannelConfig channelConfig = null;
            ClientConfig clientConfig = getClientConfigCatching(database);

            if (clientConfig == null) {
                try {
                    channelConfig = Database.readChannelConfig(Paths.get(Database.channelProperties));
                } catch (Database.InvalidConfigException e) {
                    ExceptionHandler.onInvalidChannelConfig(e);
                }

                // don't change database root when channel.properties is not present in current directory
                if (channelConfig != null) {
                    database = new Database(Paths.get(".."));
                    clientConfig = getClientConfigCatching(database);
                }
            }

            PacketEncoder packetEncoder = new PacketEncoder();
            Executor executor = Runnable::run;
            Console console = new Console();
            Observable<Void> applicationExitEvent = new Observable<>();

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

            applicationExitEvent.onNext(null);
        } catch (Exception e) {
            ExceptionHandler.onUnknownException(e);
        }
    }

    private static ClientConfig getClientConfigCatching(Database database) {
        try {
            return database.getClientConfig();
        } catch (Database.InvalidConfigException e) {
            ExceptionHandler.onInvalidClientConfig(e);

            // reaching this line is impossible because ExceptionHandler methods call System.exit()
            throw new AssertionError();
        }
    }
}
