package org.reactivestreams;

/**
 * A {@link Handle} represents a one-to-one lifecycle of a {@link Listener} subscribing to a {@link Source}.
 * <p>
 * It can only be used once by a single {@link Listener}.
 * <p>
 * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
 *
 */
public interface Handle {
    /**
     * No events will be sent by a {@link Source} until demand is signaled via this method.
     * <p>
     * It can be called however often and whenever needed.
     * <p>
     * Whatever has been signalled can be sent by the {@link Source} so only signal demand for what can be safely handled.
     * 
     * @param n
     */
    public void request(int n);

    /**
     * Request the {@link Source} to stop sending data and clean up resources.
     * <p>
     * Data may still be sent to meet previously signalled demand after calling cancel as this request is asynchronous.
     */
    public void cancel();
}
