package pl.pawelkielb.fchat.packets;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record SendFilePacket(UUID channel, String name, long size) implements Packet {
    public SendFilePacket {
        requireNonNull(channel);
        requireNonNull(name);
        if (size < 1) {
            throw new IllegalArgumentException("The size cannot be less than 1");
        }
    }
}
