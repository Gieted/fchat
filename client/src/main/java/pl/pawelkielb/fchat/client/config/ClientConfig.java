package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.data.Name;

public record ClientConfig(Name username, String serverHost, int serverPort) {
    public ClientConfig {
        if (username == null || serverHost == null) {
            throw new NullPointerException();
        }
    }

    public static ClientConfig defaults() {
        return new ClientConfig(Name.of("Guest"), "localhost", 1337);
    }
}
