package pl.pawelkielb.fchat.client.packets;

import pl.pawelkielb.fchat.client.Name;

import java.util.UUID;

public record NotifyPacket(
        UUID packetId,
        UUID channelId,
        Name channelName,
        Name recipient
) implements Packet {

    public NotifyPacket(UUID channelId, Name channelName, Name recipient) {
        this(UUID.randomUUID(), channelId, channelName, recipient);
    }
}
