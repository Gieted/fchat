package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Observable;

import java.util.function.Consumer;

public class AsyncStream<R, T> {
    private final Observable<R> producer;
    private final Observable<T> consumer;

    public AsyncStream(Observable<R> producer, Observable<T> consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    public void requestNext(R requestMessage) {
        producer.onNext(requestMessage);
    }

    public void subscribe(R initialRequest,
                          Consumer<T> observer,
                          Runnable completionListener,
                          Consumer<Exception> exceptionHandler) {

        consumer.subscribe(observer, completionListener, exceptionHandler);
        requestNext(initialRequest);
    }

    public void close() {
        producer.complete();
    }
}
