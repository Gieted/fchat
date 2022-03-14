package pl.pawelkielb.fchat.client.packets;

import java.util.UUID;

public record RequestLivePacket(
        UUID channel
) implements Packet {
}
