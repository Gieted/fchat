package pl.pawelkielb.fchat.packets;

import java.util.UUID;

public record RequestMessagesPacket(
        UUID channel,
        int count
) implements Packet {
}
