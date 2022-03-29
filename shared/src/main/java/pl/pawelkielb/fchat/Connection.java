package pl.pawelkielb.fchat;

import pl.pawelkielb.fchat.packets.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
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

    private final TaskQueue taskQueue = new TaskQueue();

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

    public CompletableFuture<Void> send(Packet packet) {
        return taskQueue.runSuspend(task -> workerThreads.execute(r(() -> {
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
                        outputStream.write(intToBytes(0));
                        logger.info("Sent packet: null");
                        task.complete(null);
                        return;
                    }

                    outputStream.write(intToBytes(packetBytes.length));
                    outputStream.write(packetBytes);
                    logger.info("Sent packet: " + packet);
                    task.complete(null);
                } catch (IOException e) {
                    task.completeExceptionally(new DisconnectedException());
                }
            }));
        })));
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

    public CompletableFuture<Void> sendBytes(byte[] bytes) {
        return taskQueue.runSuspend(task -> ioThreads.execute(r(() -> {
            connect();

            try {
                OutputStream output = socket.getOutputStream();
                output.write(intToBytes(bytes.length));
                output.write(bytes);
                logger.info(String.format("Sent %d bytes", bytes.length));
                task.complete(null);
            } catch (IOException e) {
                task.completeExceptionally(e);
            }
        })));
    }

    public CompletableFuture<byte[]> readBytes() {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        ioThreads.execute(r(() -> {
            connect();
            
            try {
                InputStream input = socket.getInputStream();
                int arraySize = intFromBytes(input.readNBytes(4));
                byte[] bytes = input.readNBytes(arraySize);
                logger.info(String.format("Read %d bytes", arraySize));
                future.complete(bytes);
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        }));

        return future;
    }
}
