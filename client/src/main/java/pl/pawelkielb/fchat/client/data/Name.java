package pl.pawelkielb.fchat.client.data;

// cannot be a record, because of private constructor
public final class Name {
    private final String value;

    private Name(String value) {
        this.value = value;
    }

    public static Name of(String name) {
        if (name.contains(",")) {
            throw new IllegalArgumentException("Name cannot contain commas");
        }
        return new Name(name);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}