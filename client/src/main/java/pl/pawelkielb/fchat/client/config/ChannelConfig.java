package pl.pawelkielb.fchat.client.config;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record ChannelConfig(UUID id) {
    public ChannelConfig {
        requireNonNull(id);
    }
}
