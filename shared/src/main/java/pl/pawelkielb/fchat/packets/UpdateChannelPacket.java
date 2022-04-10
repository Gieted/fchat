package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record UpdateChannelPacket(UUID channel, Name name, List<Name> members) implements Packet {
    public UpdateChannelPacket {
        requireNonNull(channel);
        requireNonNull(members);
    }
}
