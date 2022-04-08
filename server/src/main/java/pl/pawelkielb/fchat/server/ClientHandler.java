package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.Observable;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static pl.pawelkielb.fchat.Exceptions.c;

public class ClientHandler {
    private final Database database;
    private final Connection connection;
    private Name username;
    private final MessageManager messageManager;

    public ClientHandler(Database database, Connection connection, MessageManager messageManager) {
        this.database = database;
        this.connection = connection;
        this.messageManager = messageManager;
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
            messageManager.pushMessage(sendMessagePacket.channel(), sendMessagePacket.message());
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
            AtomicLong totalSize = new AtomicLong(0);
            saveFileControl.readyEvent().subscribe(c(() -> {
                connection.sendPacket(null);
                connection.readBytes().thenAccept(nextBytes -> {
                    totalSize.addAndGet(nextBytes.length);
                    if (nextBytes.length != 0) {
                        fileBytes.onNext(nextBytes);
                    } else {
                        fileBytes.complete();
                    }
                });
            }));
            saveFileControl.completion().thenAccept(fileName -> {
                messageManager.pushMessage(sendFilePacket.channel(), new Message(username,
                        String.format("*file %s (%d bytes)*", fileName, totalSize.get())));
                handlePacketFuture.complete(null);
            });

        } else if (packet instanceof RequestFilePacket requestFilePacket) {
            Observable<Integer> byteRequests = new Observable<>();
            Database.GetFileResult file = database.getFile(requestFilePacket.channel(), requestFilePacket.name(), byteRequests);
            file.bytes().subscribe(nextBytes -> {
                connection.sendBytes(nextBytes);
                connection.readPacket().thenRun(() -> byteRequests.onNext(1000));
            }, () -> {
                connection.sendPacket(null);
                handlePacketFuture.complete(null);
            });
            file.size().thenAccept(size -> {
                connection.sendPacket(new SendFilePacket(requestFilePacket.channel(), requestFilePacket.name(), size));
                byteRequests.onNext(1000);
            });

        } else {
            handlePacketFuture.complete(null);
        }

        return handlePacketFuture;
    }
}
