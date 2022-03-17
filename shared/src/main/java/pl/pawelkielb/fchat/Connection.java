package pl.pawelkielb.fchat;

import pl.pawelkielb.fchat.packets.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Connection {
    private Socket socket;
    private final PacketEncoder packetEncoder;

    public Connection(PacketEncoder packetEncoder) {
        this.packetEncoder = packetEncoder;
    }

    public void connect(String address, int port) throws IOException {
        this.socket = new Socket(address, port);
    }

    private void checkConnection() {
        if (socket == null) {
            throw new IllegalStateException("Not connected yet");
        }
    }

    private static int intFromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private static byte[] intToBytes(int integer) {
        return ByteBuffer.allocate(4).putInt(integer).array();
    }

    public void send(Packet packet) throws IOException {
        checkConnection();

        OutputStream outputStream = socket.getOutputStream();

        if (packet == null) {
            outputStream.write(0);
        }

        byte[] packetBytes = packetEncoder.toBytes(packet);
        outputStream.write(intToBytes(packetBytes.length));
        socket.getOutputStream().write(packetBytes);
    }

    public Packet read() throws IOException {
        checkConnection();

        InputStream input = socket.getInputStream();
        int packetSize = intFromBytes(input.readNBytes(4));

        // null packet
        if (packetSize == 0) {
            return null;
        }

        byte[] packetBytes = input.readNBytes(packetSize);

        return packetEncoder.decode(packetBytes);
    }
}
