package pl.pawelkielb.fchat.data;

import static java.util.Objects.requireNonNull;

public record Message(Name author, String content) {
    public Message {
        requireNonNull(author);
        if (content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be blank");
        }

        if (content.length() > 10000) {
            throw new IllegalArgumentException("A message content cannot be longer than 10000 characters");
        }
    }
}
