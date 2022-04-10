package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Observable;

import java.util.function.Consumer;


/**
 * Represents a stream of data. A subscriber must request the data before receiving it.
 * When subscribing, the initial request will be made and the observer will receive a first data chunk when it's ready.
 * Then the subscriber can request more data using {@link #requestNext(R)}.
 * You can also close the stream using {@link #close()} which will release the resources.
 * After closing no more data will be recieved.<br/><br/>
 *
 * @param <R> type of requests
 * @param <T> type of the received data
 */
public class AsyncStream<R, T> {
    private final Observable<R> producer;
    private final Observable<T> consumer;
    private boolean subscribed = false;

    /**
     * You should subscribe to the producer and push new values to the consumer when requested.
     *
     * @param producer an observable of requests
     * @param consumer an observable to output the data to
     */
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

        if (subscribed) {
            throw new IllegalStateException("Cannot subscribe twice");
        }

        subscribed = true;
        consumer.subscribe(observer, completionListener, exceptionHandler);
        requestNext(initialRequest);
    }

    public void subscribe(R initialRequest, Consumer<T> observer, Runnable completionListener) {
        subscribe(initialRequest, observer, completionListener, null);
    }

    public void close() {
        producer.complete();
    }
}
