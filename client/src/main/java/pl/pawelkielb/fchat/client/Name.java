package pl.pawelkielb.fchat.client;

public record Name(String value) {
    public Name {
        if (value.contains(",")) {
            throw new IllegalArgumentException("Name cannot contain commas");
        }
    }
}
