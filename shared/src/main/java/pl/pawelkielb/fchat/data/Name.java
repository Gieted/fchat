package pl.pawelkielb.fchat.data;

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
        if (name.contains("=")) {
            throw new IllegalArgumentException("Name cannot contain equals symbol");
        }
        if (name.contains("\n")) {
            throw new IllegalArgumentException("Name cannot contain new line characters");
        }
        return new Name(name);
    }

    public static boolean isValid(String name) {
        try {
            Name.of(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Name other) {
            return this.value.equals(other.value);
        }

        if (obj instanceof String other) {
            return this.value.equals(other);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
