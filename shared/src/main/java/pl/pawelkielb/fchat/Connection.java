package pl.pawelkielb.fchat;

import pl.pawelkielb.fchat.packets.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static pl.pawelkielb.fchat.Exceptions.*;


public class Connection {
    private final PacketEncoder packetEncoder;
    private Socket socket;
    private final Executor workerThreads;
    private final Executor ioThreads;
    private final Logger logger;
    private String address;
    private int port;
    private Observable<Void> applicationExitEvent;
    private ReentrantLock readLock = new ReentrantLock();

    private final TaskQueue taskQueue = new TaskQueue();

    public final static byte[] nullPacket = new byte[0];

    public Connection(PacketEncoder packetEncoder,
                      String address,
                      int port,
                      Executor workerThreads,
                      Executor ioThreads,
                      Logger logger,
                      Observable<Void> applicationExitEvent) {


        this(packetEncoder, null, workerThreads, ioThreads, logger);
        this.address = address;
        this.port = port;
        this.applicationExitEvent = applicationExitEvent;
    }

    public Connection(PacketEncoder packetEncoder,
                      Socket socket,
                      Executor workerThreads,
                      Executor ioThreads,
                      Logger logger) {

        this.packetEncoder = packetEncoder;
        this.socket = socket;
        this.workerThreads = workerThreads;
        this.ioThreads = ioThreads;
        this.logger = logger;
    }

    public static class NetworkException extends RuntimeException {
    }

    /**
     * @param bytes an array of bytes to send
     * @return a future that'll complete when all bytes will be sent.
     * Might complete exceptionally with the following exceptions:
     * <li>NetworkException - if network fails
     * <li>DisconnectedException - if the other party disconnects
     */
    public CompletableFuture<Void> sendBytes(byte[] bytes) {
        return taskQueue.runSuspend(task -> sendBytesInternal(bytes).thenRun(() -> {
            logger.info(String.format("Sent %d bytes", bytes.length));
            task.complete(null);
        }).exceptionally(vf(task::completeExceptionally)));
    }

    /**
     * @return a future resolving to read bytes.
     * Might complete exceptionally with the following exceptions:
     * <li>NetworkException - if network fails
     * <li>DisconnectedException - if the other party disconnects
     */
    public CompletableFuture<byte[]> readBytes() {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        readBytesInternal().thenAccept(bytes -> {
            logger.info(String.format("Received %d bytes", bytes.length));
            future.complete(bytes);
        }).exceptionally(vf(future::completeExceptionally));

        return future;
    }

    public CompletableFuture<Void> sendPacket(Packet packet) {
        return taskQueue.runSuspend(task -> {
            if (packet == null) {
                sendBytesInternal(nullPacket).thenRun(() -> {
                    logger.info("Sent packet: null");
                    task.complete(null);
                }).exceptionally(vf(task::completeExceptionally));
            } else {
                workerThreads.execute(() -> {
                    byte[] packetBytes = packetEncoder.toBytes(packet);
                    sendBytesInternal(packetBytes).thenRun(() -> {
                        logger.info("Sent packet: " + packet);
                        task.complete(null);
                    }).exceptionally(vf(task::completeExceptionally));
                });
            }
        });
    }

    public CompletableFuture<Packet> readPacket() {
        CompletableFuture<Packet> future = new CompletableFuture<>();
        readBytesInternal().thenAccept(bytes -> {
            if (bytes.length == 0) {
                logger.info("Received packet: null");
                future.complete(null);
                return;
            }

            workerThreads.execute(() -> {
                Packet packet = packetEncoder.decode(bytes);
                logger.info("Received packet: " + packet);
                future.complete(packet);
            });
        }).exceptionally(vf(future::completeExceptionally));

        return future;
    }

    private static int intFromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private static byte[] intToBytes(int integer) {
        return ByteBuffer.allocate(4).putInt(integer).array();
    }

    private void connect() throws IOException {
        if (socket == null) {
            socket = new Socket(address, port);
            applicationExitEvent.subscribe(c(() -> taskQueue.run(r(socket::close))));
        }
    }

    private CompletableFuture<Void> sendBytesInternal(byte[] bytes) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        ioThreads.execute(r(() -> {
            connect();

            try {
                OutputStream output = socket.getOutputStream();
                output.write(intToBytes(bytes.length));
                output.write(bytes);

                future.complete(null);
            } catch (IOException e) {
                future.completeExceptionally(new DisconnectedException());
            }
        }));

        return future;
    }

    private CompletableFuture<byte[]> readBytesInternal() {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        if (!readLock.tryLock()) {
            throw new ConcurrentReadException();
        }

        ioThreads.execute(r(() -> {
            connect();

            try {
                InputStream input = socket.getInputStream();
                byte[] arraySizeBytes = input.readNBytes(4);

                if (arraySizeBytes.length < 4) {
                    readLock = new ReentrantLock();
                    future.completeExceptionally(new DisconnectedException());
                    return;
                }

                int arraySize = intFromBytes(arraySizeBytes);
                byte[] bytes = input.readNBytes(arraySize);

                readLock = new ReentrantLock();
                future.complete(bytes);
            } catch (IOException e) {
                readLock = new ReentrantLock();
                future.completeExceptionally(new DisconnectedException());
            }
        }));

        return future;
    }
}
