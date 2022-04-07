package pl.pawelkielb.fchat.packets;

import java.util.UUID;

public record RequestMessagesPacket(
        UUID channel,
        int count
) implements Packet {
    public RequestMessagesPacket {
        if (count < 1) {
            throw new IllegalArgumentException("count might not be smaller than 1");
        }
    }
}
