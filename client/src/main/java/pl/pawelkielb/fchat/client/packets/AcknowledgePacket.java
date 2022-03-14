package pl.pawelkielb.fchat.client.packets;

import java.util.UUID;

public record AcknowledgePacket(
     UUID packetId
) implements Packet {
}
