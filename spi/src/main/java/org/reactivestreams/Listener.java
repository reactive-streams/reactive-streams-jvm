package org.reactivestreams;

/**
 * Will receive call to {@link #onListen(Handle)} once after passing an instance of {@link Listener} to {@link Source#listen(Listener)}.
 * <p>
 * No further notifications will be received until {@link Handle#request(int)} is called.
 * <p>
 * After signaling demand:
 * <ul>
 * <li>One or more invocations of {@link #onNext(Object)} up to the maximum number defined by {@link Handle#request(int)}</li>
 * <li>Single invocation of {@link #onError(Throwable)} or {@link #onCompleted()} which signals a terminal state after which no further events will be sent.
 * </ul>
 * <p>
 * Demand can be signalled via {@link Handle#request(int)} whenever the {@link Listener} instance is capable of handling more.
 *
 * @param <T>
 */
public interface Listener<T> {
    /**
     * Invoked after calling {@link Source#listen(Listener)}.
     * <p>
     * No data will start flowing until {@link Handle#request(int)} is invoked.
     * <p>
     * It is the resonsibility of this {@link Listener} instance to call {@link Handle#request(int)} whenever more data is wanted.
     * <p>
     * The {@link Source} will send notifications only in response to {@link Handle#request(int)}.
     * 
     * @param s
     *            {@link Handle} that allows requesting data via {@link Handle#request(int)}
     */
    public void onListen(Handle s);

    /**
     * Data notification sent by the {@link Source} in response to requests to {@link Handle#request(int)}.
     * 
     * @param t
     */
    public void onNext(T t);

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link Handle#request(int)} is invoked again.
     * 
     * @param t
     */
    public void onError(Throwable t);

    /**
     * Successful terminal state.
     * <p>
     * No further events will be sent even if {@link Handle#request(int)} is invoked again.
     */
    public void onComplete();
}
