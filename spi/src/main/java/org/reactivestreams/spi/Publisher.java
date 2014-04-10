package org.reactivestreams.spi;

/**
 * A Publisher is a source of elements of a given type. One or more {@link org.reactivestreams.spi.Subscriber Subscriber} may be connected
 * to this Publisher in order to receive the published elements, contingent on availability of these
 * elements as well as the presence of demand signaled by the Subscriber via {@link org.reactivestreams.spi.Subscription#requestMore(int) requestMore}.
 */
public interface Publisher<T> {
  
  /**
   * Subscribe the given {@link org.reactivestreams.spi.Subscriber Subscriber} to this Publisher. A Subscriber can at most be subscribed once
   * to a given Publisher, and to at most one Publisher in total.
   * @param subscriber The subscriber to register with this publisher.
   */
  public void subscribe(Subscriber<T> subscriber);
}
