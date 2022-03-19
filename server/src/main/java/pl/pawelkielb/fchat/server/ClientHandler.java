package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.packets.LoginPacket;
import pl.pawelkielb.fchat.packets.Packet;

import java.util.concurrent.CompletableFuture;

public class ClientHandler {
    private boolean loggedIn = false;
    private final Database database;
    private final Connection connection;

    public ClientHandler(Database database, Connection connection) {
        this.database = database;
        this.connection = connection;
    }

    public CompletableFuture<Void> handlePacket(Packet packet) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!loggedIn) {
            if (packet instanceof LoginPacket loginPacket) {
                database.listUpdatePackets(loginPacket.username())
                        .subscribe(connection::send, () -> future.complete(null));

                loggedIn = true;
            }
        } else {

        }

        return future;
    }
}
