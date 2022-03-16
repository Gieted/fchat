package pl.pawelkielb.fchat.client.exceptions;

import java.util.function.IntFunction;

public abstract class Exceptions {
    @FunctionalInterface
    public interface IntFunction_WithExceptions<R, E extends Exception> {
        R apply(int value) throws E;
    }

    public static <R, E extends Exception> IntFunction<R> u(IntFunction_WithExceptions<R, E> fn) {
        return (t) -> {
            try {
                return fn.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
