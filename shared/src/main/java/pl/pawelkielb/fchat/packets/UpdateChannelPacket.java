package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

import java.util.List;
import java.util.UUID;

public record UpdateChannelPacket(
        UUID channel,
        Name name,
        List<Name> members
) implements Packet {
}
