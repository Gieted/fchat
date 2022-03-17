package pl.pawelkielb.fchat.packets;

import java.util.UUID;

public record RequestLivePacket(
        UUID channel
) implements Packet {
}
