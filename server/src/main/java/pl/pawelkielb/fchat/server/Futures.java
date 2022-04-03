package pl.pawelkielb.fchat.server;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public abstract class Futures {
    public static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> collection) {
        return CompletableFuture.allOf(collection.toArray(new CompletableFuture[0]));
    }
}
