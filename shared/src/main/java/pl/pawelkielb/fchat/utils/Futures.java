package pl.pawelkielb.fchat.utils;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public abstract class Futures {
    public static <T> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> collection) {
        return CompletableFuture.allOf(collection.toArray(new CompletableFuture[0]));
    }
}
