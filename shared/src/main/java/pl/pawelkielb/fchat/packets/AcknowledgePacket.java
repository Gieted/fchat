package pl.pawelkielb.fchat.packets;

import java.util.UUID;

public record AcknowledgePacket(
     UUID packetId
) implements Packet {
}
