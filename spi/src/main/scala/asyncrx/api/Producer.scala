package asyncrx
package api

/**
 * A Producer is the logical source of elements of a given type. 
 * The underlying implementation is done by way of a [[asyncrx.spi.Publisher]].
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
   */
  def getPublisher: spi.Publisher[T]
}

/**
 * A Consumer is the logical sink of elements of a given type.
 * The underlying implementation is done by way of a [[asyncrx.spi.Subscriber]].
 * This interface is the user-level API for a sink while a Subscriber is the SPI.
 * 
 * Implementations of this interface will typically offer domain- or language-specific
 * methods for transforming or otherwise interacting with the stream of elements.
 */
trait Consumer[T] {
  
  /**
   * Get the underlying Subscriber for this Consumer. This method should only be used by
   * implementations of this API.
   */
  def getSubscriber: spi.Subscriber[T]
}
