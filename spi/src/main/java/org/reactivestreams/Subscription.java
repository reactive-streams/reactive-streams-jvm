package org.reactivestreams;

/**
 * A {@link Subscription} represents a one-to-one lifecycle of a {@link Subscriber} subscribing to a {@link Publisher}.
 * <p>
 * It can only be used once by a single {@link Subscriber}.
 * <p>
 * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
 *
 */
public interface Subscription {
    /**
     * No events will be sent by a {@link Publisher} until demand is signalled via this method.
     * <p>
     * It can be called however often and whenever needed.
     * <p>
     * Whatever has been signalled can be sent by the {@link Publisher} so only signal demand for what can be safely handled.
     * 
     * @param n
     */
    public void signalAdditionalDemand(int n);

    /**
     * Request the {@link Publisher} to stop sending data and clean up resources.
     * <p>
     * Data may still be sent to meet previously signalled demand after calling cancel as this request is asynchronous.
     */
    public void cancel();
}
