package pl.pawelkielb.fchat.packets;

public record SendFilePacket(String name, long size) implements Packet {
}
