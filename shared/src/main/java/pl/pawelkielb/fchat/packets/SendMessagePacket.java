package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Message;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record SendMessagePacket(UUID channel, Message message) implements Packet {
    public SendMessagePacket {
        requireNonNull(channel);
        requireNonNull(message);
    }
}
