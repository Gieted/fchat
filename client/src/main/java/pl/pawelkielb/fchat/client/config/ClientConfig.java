package pl.pawelkielb.fchat.client.config;

import pl.pawelkielb.fchat.data.Name;

public record ClientConfig(Name username, String serverHost, int serverPort) {

    public static ClientConfig defaults() {
        return new ClientConfig(Name.of("Guest"), "localhost", 1337);
    }
}
