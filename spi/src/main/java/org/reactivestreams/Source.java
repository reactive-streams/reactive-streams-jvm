package org.reactivestreams;

public interface Source<T> {

    /**
     * Request {@link Source} to start streaming data.
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Listener}.
     * <p>
     * Each {@link Handle} will work for only a single {@link Listener}.
     * <p>
     * A {@link Listener} should only subscribe once to a single {@link Source}.
     * 
     * @param s
     */
    public void listen(Listener<T> s);
}
