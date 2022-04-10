package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

import static java.util.Objects.requireNonNull;

public record LoginPacket(Name username) implements Packet {
    public LoginPacket {
        requireNonNull(username);
    }
}
