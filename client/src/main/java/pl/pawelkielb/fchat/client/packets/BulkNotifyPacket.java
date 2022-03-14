package pl.pawelkielb.fchat.client.packets;

import java.util.Set;

public record BulkNotifyPacket(Set<NotifyPacket> notifyPackets) implements Packet {
}
