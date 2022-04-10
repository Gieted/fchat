package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.Logger;
import pl.pawelkielb.fchat.PacketEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static pl.pawelkielb.fchat.Functions.r;


public class Main {
    public static void nextPacket(Connection connection,
                                  ClientHandler clientHandler) {

        connection.readPacket().thenAccept(packet ->
                clientHandler.handlePacket(packet).thenRun(() ->
                        nextPacket(connection, clientHandler)));
    }

    public static void startServer(int port, Path databaseRoot) throws IOException {
        int cpusCount = Runtime.getRuntime().availableProcessors();
        Executor workerThreads = Executors.newFixedThreadPool(Math.min(cpusCount, 32));
        Executor ioThreads = Executors.newFixedThreadPool(1000);

        PacketEncoder packetEncoder = new PacketEncoder();
        Logger logger = new ConsoleLogger(ioThreads);
        Database database = new Database(workerThreads, ioThreads, databaseRoot, packetEncoder, logger);
        MessageManager messageManager = new MessageManager(database);

        ServerSocket server = new ServerSocket(port);

        ioThreads.execute(r(() -> {
            // noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = server.accept();
                workerThreads.execute(() -> {
                    Connection connection = new Connection(packetEncoder, socket, workerThreads, ioThreads, logger);
                    ClientHandler clientHandler = new ClientHandler(database, connection, messageManager, workerThreads);
                    nextPacket(connection, clientHandler);
                });
            }
        }));
    }

    public static void main(String[] args) throws IOException {
        startServer(1337, Paths.get("."));
    }
}
