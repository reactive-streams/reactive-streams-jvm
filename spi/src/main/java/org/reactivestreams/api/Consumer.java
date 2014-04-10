package org.reactivestreams.api;

import org.reactivestreams.spi.Subscriber;

/**
 * A Consumer is the logical sink of elements of a given type.
 * The underlying implementation is done by way of a {@link org.reactivestreams.spi.Subscriber Subscriber}.
 * This interface is the user-level API for a sink while a Subscriber is the SPI.
 * <p>
 * Implementations of this interface will typically offer domain- or language-specific
 * methods for transforming or otherwise interacting with the stream of elements.
 */
public interface Consumer<T> {
  
  /**
   * Get the underlying {@link org.reactivestreams.spi.Subscriber Subscriber} for this Consumer. This method should only be used by
   * implementations of this API.
   * @return the underlying subscriber for this consumer
   */
  public Subscriber<T> getSubscriber();
}