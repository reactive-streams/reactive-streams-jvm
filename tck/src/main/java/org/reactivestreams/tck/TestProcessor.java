package org.reactivestreams.tck;

import org.reactivestreams.Listener;
import org.reactivestreams.Source;

/**
 * The TCK uses this to pull together the 2 sides of {@link Source} and {@link Listener}.
 */
public interface TestProcessor<I, O> extends Source<I>, Listener<O> {

    Listener<O> getSubscriber();

    Source<I> getPublisher();

}
