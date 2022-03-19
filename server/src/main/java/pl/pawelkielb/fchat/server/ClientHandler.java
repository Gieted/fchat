package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;
import pl.pawelkielb.fchat.utils.Futures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientHandler {
    private final Database database;
    private final Connection connection;
    private Name username;

    public ClientHandler(Database database, Connection connection) {
        this.database = database;
        this.connection = connection;
    }

    public CompletableFuture<Void> handlePacket(Packet packet) {
        CompletableFuture<Void> handlePacketFuture = new CompletableFuture<>();

        if (username == null) {
            if (packet instanceof LoginPacket loginPacket) {
                database.listUpdatePackets(loginPacket.username())
                        .subscribe(connection::send, () -> handlePacketFuture.complete(null));

                username = loginPacket.username();
            } else {
                handlePacketFuture.complete(null);
            }

            return handlePacketFuture;
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
            Futures.allOf(futures).thenRun(() -> handlePacketFuture.complete(null));
        }

        if (packet instanceof AcknowledgePacket acknowledgePacket) {
            database.deleteUpdatePacket(username, acknowledgePacket.packetId())
                    .thenRun(() -> handlePacketFuture.complete(null));
        }

        return handlePacketFuture;
    }
}
