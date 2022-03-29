package pl.pawelkielb.fchat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Observable<T> {
    private final List<Consumer<T>> observers = new ArrayList<>();
    private final List<Runnable> completionListeners = new ArrayList<>();
    private final List<Consumer<Exception>> exceptionListener = new ArrayList<>();

    public void subscribe(Consumer<T> observer, Runnable onComplete, Consumer<Exception> exceptionHandler) {
        observers.add(observer);
        if (onComplete != null) {
            completionListeners.add(onComplete);
        }
        if (exceptionHandler != null) {
            exceptionListener.add(exceptionHandler);
        }
    }

    public void subscribe(Consumer<T> observer, Runnable onComplete) {
        subscribe(observer, onComplete, null);
    }

    public void subscribe(Consumer<T> observer) {
        subscribe(observer, null);
    }

    public void onNext(T next) {
        observers.forEach(it -> it.accept(next));
    }

    public void onException(Exception e) {
        exceptionListener.forEach(it -> it.accept(e));
    }

    public void complete() {
        completionListeners.forEach(Runnable::run);
    }
}
