package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

import java.util.UUID;

public record ChannelUpdatedPacket(UUID packetID, UUID channel, Name name) implements Packet {
    public static ChannelUpdatedPacket withRandomUUID(UUID channel, Name name) {
        return new ChannelUpdatedPacket(UUID.randomUUID(), channel, name);
    }
}
