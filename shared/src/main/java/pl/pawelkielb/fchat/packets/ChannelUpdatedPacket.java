package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

import java.util.UUID;

public record ChannelUpdatedPacket(UUID channel, Name name) implements Packet {
}
