package pl.pawelkielb.fchat;

public abstract class Exceptions {
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void throwAsUnchecked(Throwable exception) throws E {
        throw (E) exception;
    }

    @FunctionalInterface
    public interface IntFunction_WithExceptions<R, E extends Exception> {
        R apply(int value) throws E;
    }

    @FunctionalInterface
    public interface Runnable_WithExceptions<E extends Exception> {
        void run() throws E;
    }

    @FunctionalInterface
    public interface Consumer_WithExceptions<T, E extends Exception> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface Function_WithExceptions<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    public interface Supplier_WithExceptions<R, E extends Exception> {
        R get() throws E;
    }
}
