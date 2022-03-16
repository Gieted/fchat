package pl.pawelkielb.fchat.client.packets;

import pl.pawelkielb.fchat.client.Message;

import java.util.UUID;

public record SendMessagePacket(
        UUID channel,
        Message message
) implements Packet {
}
