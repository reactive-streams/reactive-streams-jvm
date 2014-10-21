package org.reactivestreams;

/**
 * A {@link Processor} represents a processing stage—which is both a {@link Subscriber}
 * and a {@link Publisher} and obeys the contracts of both.
 */
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
