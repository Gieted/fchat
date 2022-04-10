package pl.pawelkielb.fchat.packets;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record RequestMessagesPacket(UUID channel, int count) implements Packet {
    public RequestMessagesPacket {
        requireNonNull(channel);
        if (count < 1) {
            throw new IllegalArgumentException("count might not be smaller than 1");
        }
    }
}
