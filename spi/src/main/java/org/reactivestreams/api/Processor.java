package org.reactivestreams.api;

/**
 * A Processor is a stand-alone representation of a transformation for
 * elements from In to Out types. Implementations of this API will provide
 * factory methods for creating Processors and connecting them to
 * {@link org.reactivestreams.api.Producer Producer} and {@link org.reactivestreams.api.Consumer Consumer}.
 */
public interface Processor<I, O> extends Consumer<I>, Producer<O> {
}
