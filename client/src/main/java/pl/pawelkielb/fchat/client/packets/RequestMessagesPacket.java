package pl.pawelkielb.fchat.client.packets;

import java.util.UUID;

public record RequestMessagesPacket(
        UUID channel,
        int count
) implements Packet {
}
