package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.packets.LoginPacket;
import pl.pawelkielb.fchat.packets.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static pl.pawelkielb.fchat.Exceptions.c;

public class ClientHandler {
    private boolean loggedIn = false;
    private final Database database;
    private final Connection connection;

    public ClientHandler(Database database, Connection connection) {
        this.database = database;
        this.connection = connection;
    }

    public CompletableFuture<?> handlePacket(Packet packet) {
        CompletableFuture<?> future = new CompletableFuture<>();

        if (!loggedIn) {
            if (packet instanceof LoginPacket loginPacket) {
                List<CompletableFuture<?>> futures = new ArrayList<>();
                database.listUpdatePackets(loginPacket.username()).thenAccept(stream ->
                        stream.forEach(updatePacketFuture -> {
                            CompletableFuture<?> sendFuture = new CompletableFuture<>();
                            updatePacketFuture.thenAccept(c(updateChannelPacket -> {
                                connection.send(updateChannelPacket).thenRun(() -> {
                                    sendFuture.complete(null);

                                });
                            }));
                        })
                );
            }
        } else {

        }

        return future;
    }
}
