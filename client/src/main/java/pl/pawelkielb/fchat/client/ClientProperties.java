package pl.pawelkielb.fchat.client;

public record ClientProperties(Name username, String serverHost, int serverPort) {

    public static ClientProperties defaults() {
        return new ClientProperties(Name.of("Guest"), "localhost", 1337);
    }
}
