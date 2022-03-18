package pl.pawelkielb.fchat.packets;

import pl.pawelkielb.fchat.data.Name;

public record LoginPacket(Name username) implements Packet {
}
