package pl.pawelkielb.fchat.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Observable<T> {
    private final List<Consumer<T>> observers = new ArrayList<>();
    private final CompletableFuture<?> completion = new CompletableFuture<>();

    public CompletableFuture<?> subscribe(Consumer<T> observer) {
        observers.add(observer);

        return completion;
    }

    public void onNext(T next) {
        observers.forEach(it -> it.accept(next));
    }

    public void complete() {
        completion.complete(null);
    }
}
