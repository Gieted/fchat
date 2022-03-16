package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.client.data.Name;

import java.util.UUID;

public record ChannelConfig(
        UUID id,
        Name name
) {

    public static ChannelConfig withRandomId(Name name) {
        return new ChannelConfig(UUID.randomUUID(), name);
    }
}
