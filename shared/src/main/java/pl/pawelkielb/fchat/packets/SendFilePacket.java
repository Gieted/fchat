package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record SendFilePacket(UUID channel, Name name, long size) implements Packet {
    public SendFilePacket {
        requireNonNull(channel);
        requireNonNull(name);
        if (size < 1) {
            throw new IllegalArgumentException("The size cannot be less than 1");
        }
    }
}
