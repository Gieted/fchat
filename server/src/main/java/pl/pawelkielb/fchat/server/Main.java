package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.PacketEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static pl.pawelkielb.fchat.Exceptions.r;


public class Main {
    public static void nextPacket(Connection connection,
                                  ClientHandler clientHandler,
                                  Executor ioThreads,
                                  Executor workerThreads) {

        connection.read().thenAccept(packet -> {
            clientHandler.handlePacket(packet);
            nextPacket(connection, clientHandler, ioThreads, workerThreads);
        });
    }

    public static void main(String[] args) throws IOException {
        int cpusCount = Runtime.getRuntime().availableProcessors();
        Executor workerThreads = Executors.newFixedThreadPool(Math.min(cpusCount, 32));
        Executor ioThreads = Executors.newFixedThreadPool(1000);

        PacketEncoder packetEncoder = new PacketEncoder();
        Database database = new Database(workerThreads, ioThreads, Paths.get("."), packetEncoder);

        ServerSocket server = new ServerSocket(8080);

        ioThreads.execute(r(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = server.accept();
                workerThreads.execute(() -> {
                    Connection connection = new Connection(packetEncoder, socket, workerThreads, ioThreads);
                    ClientHandler clientHandler = new ClientHandler(database, connection);
                    nextPacket(connection, clientHandler, ioThreads, workerThreads);
                });
            }
        }));
    }
}
