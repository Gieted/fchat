package pl.pawelkielb.fchat;

import pl.pawelkielb.fchat.packets.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static pl.pawelkielb.fchat.Exceptions.r;

public class Connection {
    private final PacketEncoder packetEncoder;
    private Socket socket;
    private final Executor workerThreads;
    private final Executor ioThreads;
    private final Logger logger;
    private String address;
    private int port;

    private final Queue<Packet> queue = new LinkedList<>();
    private final Mutex mutex = new Mutex();

    public Connection(PacketEncoder packetEncoder,
                      String address,
                      int port,
                      Executor workerThreads,
                      Executor ioThreads,
                      Logger logger) {


        this(packetEncoder, null, workerThreads, ioThreads, logger);
        this.address = address;
        this.port = port;
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
        }
    }

    private CompletableFuture<Void> sendNext(Packet packet) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        workerThreads.execute(r(() -> {
            byte[] packetBytes;
            if (packet != null) {
                packetBytes = packetEncoder.toBytes(packet);
            } else {
                packetBytes = null;
            }

            ioThreads.execute(r(() -> {
                connect();

                try {
                    OutputStream outputStream = socket.getOutputStream();

                    if (packet == null) {
                        outputStream.write(new byte[]{0, 0, 0, 0});
                        logger.info("Sent packet: null");
                        future.complete(null);
                        return;
                    }

                    outputStream.write(intToBytes(packetBytes.length));
                    outputStream.write(packetBytes);
                    logger.info("Sent packet: " + packet);
                    future.complete(null);
                } catch (IOException e) {
                    future.completeExceptionally(new DisconnectedException());
                }
            }));
        }));

        return future;
    }

    public CompletableFuture<Void> send(Packet packet) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queue.add(packet);
        mutex.lock(() -> sendNext(queue.poll()).thenRun(() -> {
            future.complete(null);
            mutex.unlock();
        }));

        return future;
    }

    public CompletableFuture<Packet> read() {
        CompletableFuture<Packet> future = new CompletableFuture<>();
        ioThreads.execute(r(() -> {
            connect();

            byte[] packetBytes;
            try {
                InputStream input = socket.getInputStream();
                int packetSize;
                packetSize = intFromBytes(input.readNBytes(4));

                // null packet
                if (packetSize == 0) {
                    logger.info("Recieved packet: null");
                    future.complete(null);
                    return;
                }

                packetBytes = input.readNBytes(packetSize);
            } catch (IOException e) {
                future.completeExceptionally(new DisconnectedException());
                return;
            }

            workerThreads.execute(r(() -> {
                Packet packet = packetEncoder.decode(packetBytes);
                logger.info("Recieved packet: " + packet);
                future.complete(packet);
            }));
        }));

        return future;
    }
}
