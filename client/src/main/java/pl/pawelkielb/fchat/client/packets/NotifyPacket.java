package pl.pawelkielb.fchat.client.packets;

import pl.pawelkielb.fchat.client.Name;

import java.util.UUID;

public record NotifyPacket(
        UUID packetId,
        UUID channelId,
        Name channelName,
        String recipient
) implements Packet {

    public NotifyPacket(Name channelName, String recipient) {
        this(UUID.randomUUID(), UUID.randomUUID(), channelName, recipient);
    }
}
