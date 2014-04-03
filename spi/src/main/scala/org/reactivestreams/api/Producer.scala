package org.reactivestreams
package api

/**
 * A Producer is the logical source of elements of a given type. 
 * The underlying implementation is done by way of a [[org.reactivestreams.spi.Publisher]].
 * This interface is the user-level API for a source while a Publisher is the
 * SPI.
 * 
 * Implementations of this interface will typically offer domain- or language-specific
 * methods for transforming or otherwise interacting with the produced stream of elements.
 */
trait Producer[T] {
  
  /**
   * Get the underlying Publisher for this Producer. This method should only be used by
   * implementations of this API.
   * 
   * @return the underlying publisher for this producer
   */
  def getPublisher: spi.Publisher[T]
  
  /**
   * Connect the given consumer to this producer. This means that the
   * Subscriber underlying the Consumer subscribes to this Producer’s
   * underlying Publisher, which will initiate the transfer of the produced
   * stream of elements from producer to consumer until either of three things
   * happen:
   * <ul>
   * <li>The stream ends normally (no more elements available).</li>
   * <li>The producer encounters a fatal error condition.</li>
   * <li>The consumer cancels the reception of more elements.</li>
   * </ul>
   * 
   * @param consumer The consumer to register with this producer.
   */
  def produceTo(consumer: Consumer[T]): Unit
}

/**
 * A Consumer is the logical sink of elements of a given type.
 * The underlying implementation is done by way of a [[org.reactivestreams.spi.Subscriber]].
 * This interface is the user-level API for a sink while a Subscriber is the SPI.
 * 
 * Implementations of this interface will typically offer domain- or language-specific
 * methods for transforming or otherwise interacting with the stream of elements.
 */
trait Consumer[T] {
  
  /**
   * Get the underlying Subscriber for this Consumer. This method should only be used by
   * implementations of this API.
   * 
   * @return the underlying subscriber for this consumer
   */
  def getSubscriber: spi.Subscriber[T]
}
