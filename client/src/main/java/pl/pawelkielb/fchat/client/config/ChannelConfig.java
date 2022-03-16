package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.client.Name;

import java.util.UUID;

public record ChannelConfig(
        UUID id,
        Name name
) {

    public ChannelConfig(Name name) {
        this(UUID.randomUUID(), name);
    }
}
