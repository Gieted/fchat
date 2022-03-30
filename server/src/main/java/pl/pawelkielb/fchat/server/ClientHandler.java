package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.Observable;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static pl.pawelkielb.fchat.Exceptions.c;

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

    public CompletableFuture<Void> handlePacket(Packet packet) throws ProtocolException {
        CompletableFuture<Void> handlePacketFuture = new CompletableFuture<>();

        if (packet instanceof LoginPacket loginPacket) {
            username = loginPacket.username();
            handlePacketFuture.complete(null);
        } else if (packet instanceof RequestUpdatesPacket) {
            checkLoggedIn();

            database.listUpdatePackets(username)
                    .subscribe(channelUpdatedPacket -> connection.sendPacket(channelUpdatedPacket).thenRun(() ->
                            database.deleteUpdatePacket(username, channelUpdatedPacket.channel())), () -> {
                        connection.sendPacket(null);
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
                    connection.sendPacket(new SendMessagePacket(requestMessagesPacket.channel(), message)), () -> {
                connection.sendPacket(null);
                handlePacketFuture.complete(null);
            });
        } else if (packet instanceof SendFilePacket sendFilePacket) {
            Observable<byte[]> fileBytes = new Observable<>();
            Database.SaveFileControl saveFileControl = database.saveFile(sendFilePacket.channel(), sendFilePacket.name(), fileBytes);
            saveFileControl.readyEvent().subscribe(c(() -> {
                connection.sendPacket(null);
                connection.readBytes().thenAccept(nextBytes -> {
                    if (nextBytes.length != 0) {
                        fileBytes.onNext(nextBytes);
                    } else {
                        fileBytes.complete();
                    }
                });
            }));
            saveFileControl.completion().thenRun(() -> handlePacketFuture.complete(null));
        } else {
            handlePacketFuture.complete(null);
        }

        return handlePacketFuture;
    }
}
