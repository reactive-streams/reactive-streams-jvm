package org.reactivestreams;

public interface Publisher<T> {

    /**
     * Request {@link Subscription} to start streaming data.
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * <p>
     * Each {@link Subscription} will work for only a single {@link Subscriber}.
     * <p>
     * A {@link Subscriber} should only subscribe once to a single {@link Publisher}.
     * 
     * @param s
     */
    public void subscribe(Subscriber<T> s);
}
