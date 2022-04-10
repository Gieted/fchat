package pl.pawelkielb.fchat;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static pl.pawelkielb.fchat.Exceptions.throwAsUnchecked;

/**
 * This class contains helper functions that convert passed functions to a different functions.
 */
public abstract class Functions {

    /**
     * Converts an IntFunction_WithExceptions to an IntFunction.
     */
    public static <R, E extends Exception> IntFunction<R> i(Exceptions.IntFunction_WithExceptions<R, E> fn) {
        return (t) -> {
            try {
                return fn.apply(t);
            } catch (Exception e) {
                throwAsUnchecked(e);
                return null;
            }
        };
    }

    /**
     * Converts a Runnable_WithExceptions to a Runnable.
     */
    public static <E extends Exception> Runnable r(Exceptions.Runnable_WithExceptions<E> fn) {
        return () -> {
            try {
                fn.run();
            } catch (Exception e) {
                throwAsUnchecked(e);
            }
        };
    }

    /**
     * Converts a Consumer_WithExceptions to a Consumer.
     */
    public static <T, E extends Exception> Consumer<T> c(Exceptions.Consumer_WithExceptions<T, E> fn) {
        return (t) -> {
            try {
                fn.accept(t);
            } catch (Exception e) {
                throwAsUnchecked(e);
            }
        };
    }

    /**
     * Converts a Runnable_WithExceptions to a Consumer.
     */
    public static <T, E extends Exception> Consumer<T> rc(Exceptions.Runnable_WithExceptions<E> fn) {
        return (t) -> {
            try {
                fn.run();
            } catch (Exception e) {
                throwAsUnchecked(e);
            }
        };
    }

    /**
     * Converts a Function_WithExceptions to Function.
     */
    public static <T, R, E extends Exception> Function<T, R> f(Exceptions.Function_WithExceptions<T, R, E> fn) {
        return (t) -> {
            try {
                return fn.apply(t);
            } catch (Exception e) {
                throwAsUnchecked(e);
                return null;
            }
        };
    }

    /**
     * Converts a Consumer_WithExceptions to a void Function.
     */
    public static <T, E extends Exception> Function<T, Void> cvf(Exceptions.Consumer_WithExceptions<T, E> fn) {
        return (t) -> {
            try {
                fn.accept(t);
            } catch (Exception e) {
                throwAsUnchecked(e);
            }
            return null;
        };
    }

    /**
     * Converts a Supplier_WithExceptions to a Supplier.
     */
    public static <R, E extends Exception> Supplier<R> s(Exceptions.Supplier_WithExceptions<R, E> fn) {
        return () -> {
            try {
                return fn.get();
            } catch (Exception e) {
                throwAsUnchecked(e);
                return null;
            }
        };
    }
}
