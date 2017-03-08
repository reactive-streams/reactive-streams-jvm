package org.reactivestreams;

/**
 * Will receive call to {@link #onSubscribe(Subscription)} once after passing an instance of {@link Subscriber} to {@link Publisher#subscribe(Subscriber)}.
 * <p>
 * No further notifications will be received until {@link Subscription#signalAdditionalDemand(int)} is called.
 * <p>
 * After signaling demand:
 * <ul>
 * <li>One or more invocations of {@link #onNext(Object)} up to the maximum number defined by {@link Subscription#signalAdditionalDemand(int)}</li>
 * <li>Single invocation of {@link #onError(Throwable)} or {@link #onCompleted()} which signals a terminal state after which no further events will be sent.
 * </ul>
 * <p>
 * Demand can be signalled via {@link Subscription#signalAdditionalDemand(int)} whenever the {@link Subscriber} instance is capable of handling more.
 *
 * @param <T>
 */
public interface Subscriber<T> {
    /**
     * Invoked after calling {@link Publisher#subscribe(Subscriber)}.
     * <p>
     * No data will start flowing until {@link Subscription#signalAdditionalDemand(int)} is invoked.
     * <p>
     * It is the resonsibility of this {@link Subscriber} instance to call {@link Subscription#signalAdditionalDemand(int)} whenever more data is wanted.
     * <p>
     * The {@link Publisher} will send notifications only in response to {@link Subscription#signalAdditionalDemand(int)}.
     * 
     * @param s
     *            {@link Subscription} that allows requesting data via {@link Subscription#signalAdditionalDemand(int)}
     */
    public void onSubscribe(Subscription s);

    /**
     * Data notification sent by the {@link Publisher} in response to requests to {@link Subscription#signalAdditionalDemand(int)}.
     * 
     * @param t
     */
    public void onNext(T t);

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#signalAdditionalDemand(int)} is invoked again.
     * 
     * @param t
     */
    public void onError(Throwable t);

    /**
     * Successful terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#signalAdditionalDemand(int)} is invoked again.
     */
    public void onCompleted();
}
