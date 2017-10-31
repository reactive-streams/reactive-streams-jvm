/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

package org.reactivestreams.tck.flow.support;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * SyncTriggeredDemandSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs synchronously (on the Publisher's thread) and requests demand triggered from
 * "the outside" using its `triggerDemand` method and from "the inside" using the return
 * value of its user-defined `whenNext` method which is invoked to process each element.
 *
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class SyncTriggeredDemandSubscriber<T> implements Subscriber<T> {
  private Subscription subscription; // Obeying rule 3.1, we make this private!
  private boolean done = false;

  @Override public void onSubscribe(final Subscription s) {
    // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Subscription` is `null`
    if (s == null) throw null;

    if (subscription != null) { // If someone has made a mistake and added this Subscriber multiple times, let's handle it gracefully
      try {
        s.cancel(); // Cancel the additional subscription
      } catch(final Throwable t) {
        //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
      }
    } else {
      // We have to assign it locally before we use it, if we want to be a synchronous `Subscriber`
      // Because according to rule 3.10, the Subscription is allowed to call `onNext` synchronously from within `request`
      subscription = s;
    }
  }

  /**
   * Requests the provided number of elements from the `Subscription` of this `Subscriber`.
   * NOTE: This makes no attempt at thread safety so only invoke it once from the outside to initiate the demand.
   * @return `true` if successful and `false` if not (either due to no `Subscription` or due to exceptions thrown)
   */
  public boolean triggerDemand(final long n) {
    final Subscription s = subscription;
    if (s == null) return false;
    else {
      try {
        s.request(n);
      } catch(final Throwable t) {
        // Subscription.request is not allowed to throw according to rule 3.16
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.16 by throwing an exception from request.", t)).printStackTrace(System.err);
        return false;
      }
      return true;
    }
  }

  @Override public void onNext(final T element) {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onNext prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `element` is `null`
      if (element == null) throw null;

      if (!done) { // If we aren't already done
        try {
          final long need = foreach(element);
          if (need > 0) triggerDemand(need);
          else if (need == 0) {}
          else {
            done();
          }
        } catch (final Throwable t) {
          done();
          try {
            onError(t);
          } catch (final Throwable t2) {
            //Subscriber.onError is not allowed to throw an exception, according to rule 2.13
            (new IllegalStateException(this + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
          }
        }
      }
    }
  }

  // Showcases a convenience method to idempotently marking the Subscriber as "done", so we don't want to process more elements
  // herefor we also need to cancel our `Subscription`.
  private void done() {
    //On this line we could add a guard against `!done`, but since rule 3.7 says that `Subscription.cancel()` is idempotent, we don't need to.
    done = true; // If we `whenNext` throws an exception, let's consider ourselves done (not accepting more elements)
    try {
      subscription.cancel(); // Cancel the subscription
    } catch(final Throwable t) {
      //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
      (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
    }
  }

  // This method is left as an exercise to the reader/extension point
  // Don't forget to call `triggerDemand` at the end if you are interested in more data,
  // a return value of < 0 indicates that the subscription should be cancelled,
  // a value of 0 indicates that there is no current need,
  // a value of > 0 indicates the current need.
  protected abstract long foreach(final T element);

  @Override public void onError(final Throwable t) {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onError prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Throwable` is `null`
      if (t == null) throw null;
      // Here we are not allowed to call any methods on the `Subscription` or the `Publisher`, as per rule 2.3
      // And anyway, the `Subscription` is considered to be cancelled if this method gets called, as per rule 2.4
    }
  }

  @Override public void onComplete() {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onComplete prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      // Here we are not allowed to call any methods on the `Subscription` or the `Publisher`, as per rule 2.3
      // And anyway, the `Subscription` is considered to be cancelled if this method gets called, as per rule 2.4
    }
  }
}
