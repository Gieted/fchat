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
    private String address;
    private int port;

    public Connection(PacketEncoder packetEncoder,
                      String address,
                      int port,
                      Executor workerThreads,
                      Executor ioThreads) {

        this(packetEncoder, null, workerThreads, ioThreads);
        this.address = address;
        this.port = port;
    }

    public Connection(PacketEncoder packetEncoder,
                      Socket socket,
                      Executor workerThreads,
                      Executor ioThreads) {

        this.packetEncoder = packetEncoder;
        this.socket = socket;
        this.workerThreads = workerThreads;
        this.ioThreads = ioThreads;
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
        CompletableFuture<Void> future = new CompletableFuture<>();

        ioThreads.execute(r(() -> {
            connect();
            OutputStream outputStream = socket.getOutputStream();

            if (packet == null) {
                outputStream.write(0);
                future.complete(null);
                return;
            }

            workerThreads.execute(r(() -> {
                byte[] packetBytes = packetEncoder.toBytes(packet);
                ioThreads.execute(r(() -> {
                    outputStream.write(intToBytes(packetBytes.length));
                    outputStream.write(packetBytes);
                    future.complete(null);
                }));
            }));
        }));

        return future;
    }

    public CompletableFuture<Packet> read() {
        CompletableFuture<Packet> future = new CompletableFuture<>();
        ioThreads.execute(r(() -> {
            connect();

            InputStream input = socket.getInputStream();
            int packetSize = intFromBytes(input.readNBytes(4));

            // null packet
            if (packetSize == 0) {
                future.complete(null);
                return;
            }

            workerThreads.execute(r(() -> {
                byte[] packetBytes = input.readNBytes(packetSize);

                future.complete(packetEncoder.decode(packetBytes));
            }));
        }));

        return future;
    }
}
