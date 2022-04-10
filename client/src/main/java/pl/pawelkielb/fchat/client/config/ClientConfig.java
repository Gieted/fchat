package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.data.Name;

import java.util.Objects;

public record ClientConfig(Name username, String serverHost, int serverPort) {
    public ClientConfig {
        Objects.requireNonNull(username);
        Objects.requireNonNull(serverHost);
        if (serverPort < 0) {
            throw new IllegalArgumentException("The server port must be a positive integer");
        }
    }

    public static ClientConfig defaults() {
        return new ClientConfig(Name.of("Guest"), "localhost", 1337);
    }
}
