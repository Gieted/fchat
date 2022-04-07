package pl.pawelkielb.fchat.packets;

import java.util.UUID;

public record RequestFilePacket(UUID channel, String name) implements Packet {
}
