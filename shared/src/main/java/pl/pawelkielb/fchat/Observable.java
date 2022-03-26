package pl.pawelkielb.fchat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Observable<T> {
    private final List<Consumer<T>> observers = new ArrayList<>();
    private final List<Runnable> completionListeners = new ArrayList<>();

    public void subscribe(Consumer<T> observer, Runnable onComplete) {
        observers.add(observer);
        if (onComplete != null) {
            completionListeners.add(onComplete);
        }
    }

    public void subscribe(Consumer<T> observer) {
        subscribe(observer, null);
    }

    public void onNext(T next) {
        observers.forEach(it -> it.accept(next));
    }

    public void complete() {
        completionListeners.forEach(Runnable::run);
    }

    public Observable<T> skip(int n) {
        Observable<T> result = new Observable<>();
        AtomicInteger i = new AtomicInteger(1);
        this.subscribe(it -> {
            if (i.getAndIncrement() > n) {
                result.onNext(it);
            }
        }, result::complete);

        return result;
    }
}
