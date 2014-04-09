package org.reactivestreams.spi;

/**
 * A Subscription models the relationship between a {@link org.reactivestreams.spi.Publisher Publisher} and a {@link org.reactivestreams.spi.Subscriber Subscriber}.
 * The Subscriber receives a Subscription so that it can ask for elements to be delivered
 * using {@link org.reactivestreams.spi.Subscription#requestMore(int) requestMore}. The Subscription can be disposed of by canceling it.
 */
public interface Subscription {
  
  /**
   * Cancel this subscription. The {@link org.reactivestreams.spi.Publisher Publisher} to which produced this Subscription 
   * will eventually stop sending more elements to the {@link org.reactivestreams.spi.Subscriber Subscriber} which owns
   * this Subscription. This may happen before the requested number of elements has
   * been delivered, even if the Publisher would still have more elements.
   */
  public void cancel();
  
  /**
   * Request more data from the {@link org.reactivestreams.spi.Publisher Publisher} which produced this Subscription.
   * The number of requested elements is cumulative to the number requested previously.
   * The Publisher may eventually publish up to the requested number of elements to
   * the {@link org.reactivestreams.spi.Subscriber Subscriber} which owns this Subscription.
   * @param elements The number of elements requested.
   */
  public void requestMore(int elements);
}