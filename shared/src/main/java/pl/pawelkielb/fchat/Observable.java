package pl.pawelkielb.fchat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Represent a stream of asynchronously calculated values. It's not thread-safe.
 *
 * @param <T> a type of the values
 */
public class Observable<T> {
    private final List<Consumer<T>> observers = new ArrayList<>();
    private final List<Runnable> completionListeners = new ArrayList<>();
    private final List<Consumer<Exception>> exceptionListener = new ArrayList<>();
    private boolean completed = false;

    /**
     * Subscribes to this observable.
     *
     * @param observer         a callback to call when new values are available
     * @param onComplete       a callback to call when this observable completes
     * @param exceptionHandler a callback to call, when an exception happens while calculating the values
     */
    public void subscribe(Consumer<T> observer, Runnable onComplete, Consumer<Exception> exceptionHandler) {
        if (observer != null) {
            observers.add(observer);
        }
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

    /**
     * Push a new value to this observable.
     *
     * @param next the value
     */
    public void onNext(T next) {
        observers.forEach(it -> it.accept(next));
    }

    private final static String alreadyCompleted = "Already completed";

    /**
     * Completes this observable with an exception.
     *
     * @param e the exception
     */
    public void onException(Exception e) {
        if (completed) {
            throw new IllegalStateException(alreadyCompleted);
        }

        completed = true;
        if (exceptionListener.isEmpty()) {
            Exceptions.throwAsUnchecked(e);
        }
        exceptionListener.forEach(it -> it.accept(e));
    }

    /**
     * Completes this observable.
     */
    public void complete() {
        if (completed) {
            throw new IllegalStateException(alreadyCompleted);
        }

        completed = true;
        completionListeners.forEach(Runnable::run);
    }
}
