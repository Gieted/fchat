package pl.pawelkielb.fchat.packets;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record RequestLivePacket(UUID channel) implements Packet {
    public RequestLivePacket {
        requireNonNull(channel);
    }
}
