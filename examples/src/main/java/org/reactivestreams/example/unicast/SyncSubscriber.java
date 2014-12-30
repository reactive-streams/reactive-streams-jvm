package org.reactivestreams.example.unicast;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * SyncSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs synchronously (on the Publisher's thread) and requests one element
 * at a time and invokes a user-defined method to process each element.
 *
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class SyncSubscriber<T> implements Subscriber<T> {
  private Subscription subscription; // Obeying rule 3.1, we make this private!
  private boolean done = false;

  @Override public void onSubscribe(final Subscription s) {
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
      try {
        // If we want elements, according to rule 2.1 we need to call `request`
        // And, according to rule 3.2 we are allowed to call this synchronously from within the `onSubscribe` method
        s.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
      } catch(final Throwable t) {
        // Subscription.request is not allowed to throw according to rule 3.16
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.16 by throwing an exception from cancel.", t)).printStackTrace(System.err);
      }
    }
  }

  @Override public void onNext(final T element) {
    if (!done) { // If we aren't already done
      try {
        if (foreach(element)) {
          try {
            subscription.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
          } catch(final Throwable t) {
            // Subscription.request is not allowed to throw according to rule 3.16
            (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.16 by throwing an exception from cancel.", t)).printStackTrace(System.err);
          }
        } else {
          done();
        }
      } catch(final Throwable t) {
         done(); 
        try {  
          onError(t);
        } catch(final Throwable t2) {
          //Subscriber.onError is not allowed to throw an exception, according to rule 2.13
          (new IllegalStateException(this + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
        }
      }
    }
  }

  // Showcases a convenience method to idempotently marking the Subscriber as "done", so we don't want to process more elements
  // herefor we also need to cancel our `Subscription`.
  private void done() {
    //On this line we could add a guard against `!done`, but since rule 3.7 says that `Subscription.cancel()` is idempotent, we don't need to.
    done = true; // If we `foreach` throws an exception, let's consider ourselves done (not accepting more elements)
    try {
      subscription.cancel(); // Cancel the subscription
    } catch(final Throwable t) {
      //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
      (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
    }
  }

  // This method is left as an exercise to the reader/extension point
  // Returns whether more elements are desired or not, and if no more elements are desired
  protected abstract boolean foreach(final T element);

  @Override public void onError(final Throwable t) {
     // Here we are not allowed to call any methods on the `Subscription` or the `Publisher`, as per rule 2.3
     // And anyway, the `Subscription` is considered to be cancelled if this method gets called, as per rule 2.4
  }

  @Override public void onComplete() {
     // Here we are not allowed to call any methods on the `Subscription` or the `Publisher`, as per rule 2.3
     // And anyway, the `Subscription` is considered to be cancelled if this method gets called, as per rule 2.4
  }
}