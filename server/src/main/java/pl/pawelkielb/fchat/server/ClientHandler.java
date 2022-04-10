package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Connection;
import pl.pawelkielb.fchat.Observable;
import pl.pawelkielb.fchat.data.Message;
import pl.pawelkielb.fchat.data.Name;
import pl.pawelkielb.fchat.packets.*;

import java.net.ProtocolException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import static pl.pawelkielb.fchat.Functions.*;
import static pl.pawelkielb.fchat.TransferSettings.fileChunkSizeInBytes;

/**
 * Handles a single client's requests. It's not thread-safe.
 */
public class ClientHandler {
    private final Database database;
    private final Connection connection;
    private final MessageManager messageManager;
    private final Executor workerThreads;

    private Name username;

    public ClientHandler(Database database, Connection connection, MessageManager messageManager, Executor workerThreads) {
        this.database = database;
        this.connection = connection;
        this.messageManager = messageManager;
        this.workerThreads = workerThreads;
    }

    /**
     * @param packet a packet to handle
     * @return A future that will be resolved when the packet was handled.
     * Might complete exceptionally with any {@link Exception}.
     */
    public CompletableFuture<Void> handlePacket(Packet packet) {
        CompletableFuture<Void> handlePacketFuture = new CompletableFuture<>();

        workerThreads.execute(r(() -> {
            if (packet instanceof LoginPacket loginPacket) {
                handleLoginPacket(loginPacket, handlePacketFuture);
            } else if (packet instanceof RequestUpdatesPacket) {
                handleRequestUpdatesPacket(handlePacketFuture);
            } else if (packet instanceof UpdateChannelPacket updateChannelPacket) {
                handleUpdateChannelPacket(updateChannelPacket, handlePacketFuture);
            } else if (packet instanceof SendMessagePacket sendMessagePacket) {
                handleSendMessagePacket(sendMessagePacket, handlePacketFuture);
            } else if (packet instanceof RequestMessagesPacket requestMessagesPacket) {
                handleRequestMessagesPacket(requestMessagesPacket, handlePacketFuture);
            } else if (packet instanceof SendFilePacket sendFilePacket) {
                handleSendFilePacket(sendFilePacket, handlePacketFuture);
            } else if (packet instanceof RequestFilePacket requestFilePacket) {
                handleRequestFilePacket(requestFilePacket, handlePacketFuture);
            } else {
                handlePacketFuture.complete(null);
            }
        }));

        return handlePacketFuture;
    }

    private void checkLoggedIn() throws ProtocolException {
        if (username == null) {
            throw new ProtocolException();
        }
    }

    private void handleLoginPacket(LoginPacket packet, CompletableFuture<Void> handlePacketFuture) {
        username = packet.username();
        handlePacketFuture.complete(null);
    }

    private void handleRequestUpdatesPacket(CompletableFuture<Void> handlePacketFuture)
            throws ProtocolException {

        checkLoggedIn();

        database.listChannelUpdatedPackets(username)
                .subscribe(
                        channelUpdatedPacket -> connection.sendPacket(channelUpdatedPacket).thenRun(() ->
                                database.deleteChannelUpdatedPacket(username, channelUpdatedPacket.channel())),
                        () -> {
                            connection.sendPacket(null);
                            handlePacketFuture.complete(null);
                        });
    }

    private void handleUpdateChannelPacket(UpdateChannelPacket packet, CompletableFuture<Void> handlePacketFuture)
            throws ProtocolException {

        checkLoggedIn();


        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<Name> members = new ArrayList<>(packet.members());
        members.add(this.username);
        for (var member : members) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            futures.add(future);

            Name channelName;
            if (packet.name() != null) {
                channelName = packet.name();
            } else {
                if (packet.members().size() == 1) {
                    channelName = username == member ? packet.members().get(0) : username;
                } else {
                    channelName = Name.of("Group channel");
                }
            }

            ChannelUpdatedPacket channelUpdatedPacket = new ChannelUpdatedPacket(
                    packet.channel(),
                    channelName
            );

            database.saveChannelUpdatedPacket(member, channelUpdatedPacket).thenRun(() -> future.complete(null));
        }
        Futures.allOf(futures).thenRun(() -> handlePacketFuture.complete(null));
    }

    private void handleSendMessagePacket(SendMessagePacket packet, CompletableFuture<Void> handlePacketFuture) {
        messageManager.pushMessage(packet.channel(), packet.message());
        handlePacketFuture.complete(null);
    }

    private void handleRequestMessagesPacket(RequestMessagesPacket packet, CompletableFuture<Void> handlePacketFuture) {
        database.getMessages(packet.channel(), packet.count()).subscribe(message ->
                connection.sendPacket(new SendMessagePacket(packet.channel(), message)), () -> {
            connection.sendPacket(null);
            handlePacketFuture.complete(null);
        });
    }

    private void handleSendFilePacket(SendFilePacket packet, CompletableFuture<Void> handlePacketFuture) {
        Observable<Integer> producer = new Observable<>();
        Observable<byte[]> consumer = new Observable<>();

        var saveFileFuture = database.saveFile(
                packet.channel(),
                packet.name(),
                new AsyncStream<>(producer, consumer)
        );

        AtomicLong totalSize = new AtomicLong(0);
        producer.subscribe(rc(() -> {
            connection.sendPacket(null);
            connection.readBytes().thenAccept(nextBytes -> {
                totalSize.addAndGet(nextBytes.length);
                if (nextBytes.length != 0) {
                    consumer.onNext(nextBytes);
                } else {
                    consumer.complete();
                }
            });
        }));

        saveFileFuture.thenAccept(fileName -> {
            messageManager.pushMessage(packet.channel(), new Message(username,
                    String.format("file \"%s\" (%d bytes)", fileName, totalSize.get())));
            handlePacketFuture.complete(null);
        });
    }

    private void handleRequestFilePacket(RequestFilePacket packet, CompletableFuture<Void> handlePacketFuture) {
        database.getFileSize(packet.channel(), packet.name()).thenAccept(size -> {
            connection.sendPacket(new SendFilePacket(packet.channel(), packet.name(), size));

            var file = database.getFile(packet.channel(), packet.name());
            file.subscribe(fileChunkSizeInBytes, nextBytes -> {
                connection.sendBytes(nextBytes).exceptionally(cvf(t -> file.close()));
                connection.readPacket()
                        .thenRun(() -> file.requestNext(fileChunkSizeInBytes))
                        .exceptionally(cvf(t -> file.close()));
            }, () -> {
                connection.sendBytes(new byte[0]);
                handlePacketFuture.complete(null);
            }, handlePacketFuture::completeExceptionally);
        }).exceptionally(cvf(throwable -> {
            if (throwable.getCause() instanceof NoSuchFileException) {
                connection.sendPacket(null);
                handlePacketFuture.complete(null);
            } else {
                handlePacketFuture.completeExceptionally(throwable);
            }
        }));
    }
}
