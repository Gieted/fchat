package pl.pawelkielb.fchat.data;

import static java.util.Objects.requireNonNull;

public record Message(Name author, String content) {
    public Message {
        requireNonNull(author);
        if (content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be blank");
        }
    }
}
