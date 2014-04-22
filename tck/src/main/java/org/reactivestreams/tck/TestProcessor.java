package org.reactivestreams.tck;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Publisher;

/**
 * The TCK uses this to pull together the 2 sides of {@link Publisher} and {@link Subscriber}.
 */
public interface TestProcessor<I, O> extends Publisher<I>, Subscriber<O> {

    Subscriber<O> getSubscriber();

    Publisher<I> getPublisher();

}
