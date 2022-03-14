package pl.pawelkielb.fchat.client.packets;

import pl.pawelkielb.fchat.client.Name;

import java.util.UUID;

public record SendMessagePacket(
        Name author,
        UUID channel,
        String message
) implements Packet {
}
