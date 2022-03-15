package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.packets.Packet;

import java.io.IOException;
import java.net.Socket;

public class Connection {
    private Socket socket;
    private final String address;
    private final int port;
    private final PacketEncoder packetEncoder;

    public Connection(String address, int port, PacketEncoder packetEncoder) {
        this.address = address;
        this.port = port;
        this.packetEncoder = packetEncoder;
    }

    public void connect() throws IOException {
        this.socket = new Socket(address, port);
    }

    private void checkConnection() {
        if (socket == null) {
            throw new IllegalStateException("Not connected yet");
        }
    }

    public void send(Packet packet) throws IOException {
        checkConnection();

        byte[] bytes = packetEncoder.toBytes(packet);
        socket.getOutputStream().write(bytes.length);
        socket.getOutputStream().write(bytes);
    }

    public void sendNull() throws IOException {
        socket.getOutputStream().write(0);
    }

    public void read() {
        checkConnection();
    }
}
