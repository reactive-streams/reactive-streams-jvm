package org.reactivestreams.spi

/**
 * A Subscription models the relationship between a [[Publisher]] and a [[Subscriber]].
 * The Subscriber receives a Subscription so that it can ask for elements to be delivered
 * using [[Subscription#requestMore]]. The Subscription can be disposed of by canceling it.
 */
trait Subscription {
  
  /**
   * Cancel this subscription. The [[Publisher]] to which produced this Subscription 
   * will eventually stop sending more elements to the [[Subscriber]] which owns
   * this Subscription. This may happen before the requested number of elements has
   * been delivered, even if the Publisher would still have more elements.
   */
  def cancel(): Unit
  
  /**
   * Request more data from the [[Publisher]] which produced this Subscription.
   * The number of requested elements is cumulative to the number requested previously.
   * The Publisher may eventually publish up to the requested number of elements to
   * the [[Subscriber]] which owns this Subscription.
   * 
   * @param elements The number of elements requested.
   */
  def requestMore(elements: Int): Unit
}

/**
 * A Publisher is a source of elements of a given type. One or more [[Subscriber]] may be connected
 * to this Publisher in order to receive the published elements, contingent on availability of these
 * elements as well as the presence of demand signaled by the Subscriber via [[Subscription#requestMore]].
 */
trait Publisher[T] {
  
  /**
   * Subscribe the given [[Subscriber]] to this Publisher. A Subscriber can at most be subscribed once
   * to a given Publisher, and to at most one Publisher in total.
   * 
   * @param subscriber The subscriber to register with this publisher.
   */
  def subscribe(subscriber: Subscriber[T]): Unit
}

/**
 * A Subscriber receives elements from a [[Publisher]] based on the [[Subscription]] it has.
 * The Publisher may supply elements as they become available, the Subscriber signals demand via 
 * [[Subscription#requestMore]] and elements from when supply and demand are both present.
 */
trait Subscriber[T] {
  
  /**
   * The [[Publisher]] generates a [[Subscription]] upon [[Publisher#subscribe]] and passes
   * it on to the Subscriber named there using this method. The Publisher may choose to reject
   * the subscription request by calling [[#onError]] instead.
   * 
   * @param subscription The subscription which connects this subscriber to its publisher.
   */
  def onSubscribe(subscription: Subscription): Unit
  
  /**
   * The [[Publisher]] calls this method to pass one element to this Subscriber. The element
   * must not be <code>null</code>. The Publisher must not call this method more often than
   * the Subscriber has signaled demand for via the corresponding [[Subscription]].
   * 
   * @param element The element that is passed from publisher to subscriber.
   */
  def onNext(element: T): Unit
  
  /**
   * The [[Publisher]] calls this method in order to signal that it terminated normally.
   * No more elements will be forthcoming and none of the Subscriber’s methods will be
   * called hereafter.
   */
  def onComplete(): Unit
  
  /**
   * The [[Publisher]] calls this method to signal that the stream of elements has failed
   * and is being aborted. The Subscriber should abort its processing as soon as possible.
   * No more elements will be forthcoming and none of the Subscriber’s methods will be
   * called hereafter.
   * 
   * This method is not intended to pass validation errors or similar from Publisher to Subscriber
   * in order to initiate an orderly shutdown of the exchange; it is intended only for fatal
   * failure conditions which make it impossible to continue processing further elements.
   * 
   * @param cause An exception which describes the reason for tearing down this stream.
   */
  def onError(cause: Throwable): Unit
}
