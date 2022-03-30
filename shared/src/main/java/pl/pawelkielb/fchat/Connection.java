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

import static pl.pawelkielb.fchat.Exceptions.c;
import static pl.pawelkielb.fchat.Exceptions.r;

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

    public CompletableFuture<Void> send(Packet packet) {
        return taskQueue.runSuspend(task -> {
            if (packet == null) {
                sendBytesInternal(null).thenRun(() -> task.complete(null));
            } else {
                workerThreads.execute(() -> {
                    byte[] packetBytes = packetEncoder.toBytes(packet);
                    sendBytesInternal(packetBytes).thenRun(() -> task.complete(null));
                });
            }
        });
    }

    public CompletableFuture<Packet> read() {
        CompletableFuture<Packet> future = new CompletableFuture<>();
        if (!readLock.tryLock()) {
            throw new ConcurrentReadException();
        }

        ioThreads.execute(r(() -> {
            connect();

            byte[] packetBytes;
            try {
                InputStream input = socket.getInputStream();
                int packetSize;
                byte[] packetSizeBytes = input.readNBytes(4);

                if (packetSizeBytes.length < 4) {
                    readLock = new ReentrantLock();
                    future.completeExceptionally(new DisconnectedException());
                    return;
                }

                packetSize = intFromBytes(packetSizeBytes);

                // null packet
                if (packetSize == 0) {
                    logger.info("Recieved packet: null");
                    readLock = new ReentrantLock();
                    future.complete(null);
                    return;
                }

                packetBytes = input.readNBytes(packetSize);
            } catch (IOException e) {
                readLock = new ReentrantLock();
                future.completeExceptionally(new DisconnectedException());
                return;
            }

            workerThreads.execute(r(() -> {
                Packet packet = packetEncoder.decode(packetBytes);
                logger.info("Recieved packet: " + packet);
                readLock = new ReentrantLock();
                future.complete(packet);
            }));
        }));

        return future;
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
        ioThreads.execute(r(() -> {
            connect();

            try {
                InputStream input = socket.getInputStream();
                int arraySize = intFromBytes(input.readNBytes(4));
                byte[] bytes = input.readNBytes(arraySize);
                future.complete(bytes);
            } catch (IOException e) {
                future.completeExceptionally(new DisconnectedException());
            }
        }));

        return future;
    }

    public CompletableFuture<Void> sendBytes(byte[] bytes) {
        return taskQueue.runSuspend(task -> sendBytesInternal(bytes).thenRun(() -> {
            logger.info(String.format("Sent %d bytes", bytes.length));
            task.complete(null);
        }));
    }

    public CompletableFuture<byte[]> readBytes() {
        return taskQueue.runSuspend(task -> readBytesInternal().thenAccept(bytes -> {
            logger.info(String.format("Recieved %d bytes", bytes.length));
            task.complete(bytes);
        }));
    }
}
