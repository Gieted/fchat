package pl.pawelkielb.fchat.client;

import pl.pawelkielb.fchat.client.packets.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class PacketEncoder {
    
    public byte[] toBytes(Packet packet) {
        byte[] packetBytes;
        if (packet instanceof SendMessagePacket sendMessagePacket) {
            packetBytes = toBytes(sendMessagePacket);
        } else if (packet instanceof RequestMessagesPacket requestMessagesPacket) {
            packetBytes = toBytes(requestMessagesPacket);
        } else if (packet instanceof NotifyPacket notifyPacket) {
            packetBytes = toBytes(notifyPacket);
        } else if (packet instanceof BulkNotifyPacket bulkNotifyPacket) {
            packetBytes = toBytes(bulkNotifyPacket);
        } else if (packet instanceof RequestLivePacket requestLivePacket) {
            packetBytes = toBytes(requestLivePacket);
        } else if (packet instanceof AcknowledgePacket acknowledgePacket) {
            packetBytes = toBytes(acknowledgePacket);
        } else {
            throw new IllegalArgumentException();
        }

        return packetBytes;
    }

    private static String propertiesToString(Properties properties) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try {
            properties.store(outputStreamWriter, null);
        } catch (IOException ignore) {
        }

        return outputStream.toString();
    }

    public byte[] toBytes(SendMessagePacket packet) {
        Properties properties = new Properties();
        properties.setProperty("type", "SendMessage");
        properties.setProperty("author", packet.author().value());
        properties.setProperty("channel", packet.channel().toString());
        properties.setProperty("message", packet.message());
        String packetString = propertiesToString(properties);

        return packetString.getBytes();
    }

    public byte[] toBytes(RequestMessagesPacket packet) {
        Properties properties = new Properties();
        properties.setProperty("type", "RequestMessages");
        properties.setProperty("channel", packet.channel().toString());
        properties.setProperty("count", String.valueOf(packet.count()));
        String packetString = propertiesToString(properties);

        return packetString.getBytes();
    }

    public byte[] toBytes(NotifyPacket packet) {
        Properties properties = new Properties();
        properties.setProperty("type", "Notify");
        properties.setProperty("packet_id", packet.packetId().toString());
        properties.setProperty("channel_id", packet.channelId().toString());
        properties.setProperty("channel_name", packet.channelName().value());
        properties.setProperty("recipient", packet.recipient());
        String packetString = propertiesToString(properties);

        return packetString.getBytes();
    }

    public byte[] toBytes(BulkNotifyPacket packet) {
        Properties properties = new Properties();
        properties.setProperty("type", "BulkNotify");
        properties.setProperty("count", String.valueOf(packet.notifyPackets().size()));
        int i = 0;
        for (var notifyPacket : packet.notifyPackets()) {
            properties.setProperty("packet_id" + i, notifyPacket.packetId().toString());
            properties.setProperty("channel_id" + i, notifyPacket.channelId().toString());
            properties.setProperty("channel_name" + i, notifyPacket.channelName().value());
            properties.setProperty("recipient" + i, notifyPacket.recipient());
            i++;
        }
        String packetString = propertiesToString(properties);

        return packetString.getBytes();
    }

    public byte[] toBytes(RequestLivePacket packet) {
        Properties properties = new Properties();
        properties.setProperty("type", "RequestLive");
        properties.setProperty("channel", packet.channel().toString());
        String packetString = propertiesToString(properties);

        return packetString.getBytes();
    }

    public byte[] toBytes(AcknowledgePacket packet) {
        Properties properties = new Properties();
        properties.setProperty("type", "Acknowledge");
        properties.setProperty("packet_id", packet.packetId().toString());
        String packetString = propertiesToString(properties);

        return packetString.getBytes();
    }
}
