package pl.pawelkielb.fchat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Observable<T> {
    private final List<Consumer<T>> observers = new ArrayList<>();
    private final List<T> values = new ArrayList<>();
    private final List<Runnable> completionListeners = new ArrayList<>();
    private final List<Consumer<Exception>> exceptionListener = new ArrayList<>();
    private boolean completed = false;
    private Exception exception = null;

    public void subscribe(Consumer<T> observer, Runnable onComplete, Consumer<Exception> exceptionHandler) {
        if (observer != null) {
            values.forEach(observer);
            observers.add(observer);
        }
        if (onComplete != null) {
            if (completed) {
                onComplete.run();
            }
            completionListeners.add(onComplete);
        }
        if (exceptionHandler != null) {
            if (exception != null) {
                exceptionHandler.accept(exception);
            }
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
        values.add(next);
        observers.forEach(it -> it.accept(next));
    }

    public void onException(Exception e) {
        exception = e;
        exceptionListener.forEach(it -> it.accept(e));
    }

    public void complete() {
        completed = true;
        completionListeners.forEach(Runnable::run);
    }
}
