package org.reactivestreams.spi;

/**
 * A Publisher is a source of elements of a given type. One or more [[Subscriber]] may be connected
 * to this Publisher in order to receive the published elements, contingent on availability of these
 * elements as well as the presence of demand signaled by the Subscriber via [[Subscription#requestMore]].
 */
public interface Publisher<T> {
  
  /**
   * Subscribe the given [[Subscriber]] to this Publisher. A Subscriber can at most be subscribed once
   * to a given Publisher, and to at most one Publisher in total.
   * 
   * @param subscriber The subscriber to register with this publisher.
   */
  public void subscribe(Subscriber<T> subscriber);
}
