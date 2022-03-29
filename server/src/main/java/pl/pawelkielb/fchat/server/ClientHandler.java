package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.Observable;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.net.ProtocolException;
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

    private void checkLoggedIn() throws ProtocolException {
        if (username == null) {
            throw new ProtocolException();
        }
    }

    private void readNextBytes(Observable<byte[]> fileBytes) {
        connection.readBytes().thenAccept(nextBytes -> {
            if (nextBytes.length != 0) {
                fileBytes.onNext(nextBytes);
                readNextBytes(fileBytes);
            } else {
                fileBytes.complete();
            }
        });
    }

    public CompletableFuture<Void> handlePacket(Packet packet) throws ProtocolException {
        CompletableFuture<Void> handlePacketFuture = new CompletableFuture<>();

        if (packet instanceof LoginPacket loginPacket) {
            username = loginPacket.username();
            handlePacketFuture.complete(null);
        } else if (packet instanceof RequestUpdatesPacket) {
            checkLoggedIn();

            database.listUpdatePackets(username)
                    .subscribe(channelUpdatedPacket -> connection.send(channelUpdatedPacket).thenRun(() ->
                            database.deleteUpdatePacket(username, channelUpdatedPacket.channel())), () -> {
                        connection.send(null);
                        handlePacketFuture.complete(null);
                    });
        } else if (packet instanceof UpdateChannelPacket updateChannelPacket) {
            checkLoggedIn();

            ChannelUpdatedPacket channelUpdatedPacket = new ChannelUpdatedPacket(
                    updateChannelPacket.channel(),
                    updateChannelPacket.name()
            );

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<Name> members = new ArrayList<>(updateChannelPacket.members());
            members.add(this.username);
            for (var member : members) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                futures.add(future);
                database.saveUpdatePacket(member, channelUpdatedPacket).thenRun(() -> future.complete(null));
            }
            Futures.allOf(futures).thenRun(() -> handlePacketFuture.complete(null));
        } else if (packet instanceof SendMessagePacket sendMessagePacket) {
            database.saveMessage(sendMessagePacket.channel(), sendMessagePacket.message());
            handlePacketFuture.complete(null);
        } else if (packet instanceof RequestMessagesPacket requestMessagesPacket) {
            database.getMessages(requestMessagesPacket.channel(), requestMessagesPacket.count()).subscribe(message ->
                    connection.send(new SendMessagePacket(requestMessagesPacket.channel(), message)), () -> {
                connection.send(null);
                handlePacketFuture.complete(null);
            });
        } else if (packet instanceof SendFilePacket sendFilePacket) {
            Observable<byte[]> fileBytes = new Observable<>();
            database.saveFile(sendFilePacket.channel(), sendFilePacket.name(), fileBytes);
            readNextBytes(fileBytes);
            fileBytes.subscribe(null, () -> handlePacketFuture.complete(null));
        } else {
            handlePacketFuture.complete(null);
        }

        return handlePacketFuture;
    }
}
