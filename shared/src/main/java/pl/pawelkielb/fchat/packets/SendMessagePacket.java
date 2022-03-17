package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Message;

import java.util.UUID;

public record SendMessagePacket(
        UUID channel,
        Message message
) implements Packet {
}
