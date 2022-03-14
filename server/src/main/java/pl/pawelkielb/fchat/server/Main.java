package pl.pawelkielb.fchat.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080);
        Socket socket = server.accept();
        var in = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()));

        while (true) {
            int packetSize = in.read();
            byte[] packetBytes = in.readNBytes(packetSize);
            String packetString = new String(packetBytes);
            System.out.println(packetString);
            Properties properties = new Properties();
            properties.load(new StringReader(packetString));
        }
    }
}
