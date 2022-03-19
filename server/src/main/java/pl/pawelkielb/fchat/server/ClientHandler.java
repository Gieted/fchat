package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.packets.ChannelUpdatedPacket;
import pl.pawelkielb.fchat.packets.LoginPacket;
import pl.pawelkielb.fchat.packets.Packet;
import pl.pawelkielb.fchat.packets.UpdateChannelPacket;
import pl.pawelkielb.fchat.utils.Futures;

import java.util.ArrayList;
import java.util.List;
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
        CompletableFuture<Void> handleFuture = new CompletableFuture<>();

        if (!loggedIn) {
            if (packet instanceof LoginPacket loginPacket) {
                database.listUpdatePackets(loginPacket.username())
                        .subscribe(connection::send, () -> handleFuture.complete(null));

                loggedIn = true;
            } else {
                handleFuture.complete(null);
            }

            return handleFuture;
        }

        if (packet instanceof UpdateChannelPacket updateChannelPacket) {
            ChannelUpdatedPacket channelUpdatedPacket = ChannelUpdatedPacket.withRandomUUID(
                    updateChannelPacket.channel(),
                    updateChannelPacket.name()
            );

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (var member : updateChannelPacket.members()) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                futures.add(future);
                database.saveUpdatePacket(member, channelUpdatedPacket).thenRun(() -> future.complete(null));
            }
            Futures.allOf(futures).thenRun(() -> handleFuture.complete(null));
        }

        return handleFuture;
    }
}
