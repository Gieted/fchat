package pl.pawelkielb.fchat.client;

import java.util.UUID;

public record ChannelProperties(
        UUID id,
        Name name
) {

    public ChannelProperties(Name name) {
        this(UUID.randomUUID(), name);
    }
}
