package org.reactivestreams.spi;

/**
 * A Subscription models the relationship between a [[Publisher]] and a [[Subscriber]].
 * The Subscriber receives a Subscription so that it can ask for elements to be delivered
 * using [[Subscription#requestMore]]. The Subscription can be disposed of by canceling it.
 */
public interface Subscription {
  
  /**
   * Cancel this subscription. The [[Publisher]] to which produced this Subscription 
   * will eventually stop sending more elements to the [[Subscriber]] which owns
   * this Subscription. This may happen before the requested number of elements has
   * been delivered, even if the Publisher would still have more elements.
   */
  public void cancel();
  
  /**
   * Request more data from the [[Publisher]] which produced this Subscription.
   * The number of requested elements is cumulative to the number requested previously.
   * The Publisher may eventually publish up to the requested number of elements to
   * the [[Subscriber]] which owns this Subscription.
   * 
   * @param elements The number of elements requested.
   */
  public void requestMore(int elements);
}