package pl.pawelkielb.fchat.client.packets;

import pl.pawelkielb.fchat.client.data.Name;

import java.util.UUID;

public record UpdateChannelPacket(
        UUID packetId,
        UUID channelId,
        Name channelName,
        Name recipient
) implements Packet {

    public static UpdateChannelPacket withRandomId(UUID channelId, Name channelName, Name recipient) {
        return new UpdateChannelPacket(UUID.randomUUID(), channelId, channelName, recipient);
    }
}
