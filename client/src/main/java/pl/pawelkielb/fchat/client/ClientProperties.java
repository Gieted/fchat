package pl.pawelkielb.fchat.client;

public record ClientProperties(Name username, String serverHost, int serverPort) {

    public ClientProperties() {
        // default values
        this(Name.of("Guest"), "localhost", 1337);
    }
}
