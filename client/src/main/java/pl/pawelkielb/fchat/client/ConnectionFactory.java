package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.PacketEncoder;
import pl.pawelkielb.fchat.client.config.ClientConfig;

import java.util.concurrent.Executor;

public class ConnectionFactory {
    private final PacketEncoder packetEncoder;
    private final Executor workerThreads;
    private final Executor ioThreads;

    public ConnectionFactory(PacketEncoder packetEncoder, Executor workerThreads, Executor ioThreads) {
        this.packetEncoder = packetEncoder;
        this.workerThreads = workerThreads;
        this.ioThreads = ioThreads;
    }

    public Connection create(ClientConfig clientConfig) {
        return new Connection(packetEncoder, clientConfig.serverHost(), clientConfig.serverPort(), workerThreads, ioThreads);
    }
}
