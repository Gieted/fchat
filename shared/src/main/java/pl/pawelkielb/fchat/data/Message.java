package pl.pawelkielb.fchat.data;

public record Message(Name author, String content) {
    public Message {
        if (content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be blank");
        }
    }
}
