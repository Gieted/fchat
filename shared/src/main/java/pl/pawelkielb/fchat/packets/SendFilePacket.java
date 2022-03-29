package pl.pawelkielb.fchat.packets;

import java.util.UUID;

public record SendFilePacket(UUID channel, String name, long size) implements Packet {
}
