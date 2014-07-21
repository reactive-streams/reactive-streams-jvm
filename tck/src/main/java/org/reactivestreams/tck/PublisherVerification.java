package org.reactivestreams.tck;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.Latch;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriberWithSubscriptionSupport;
import org.reactivestreams.tck.TestEnvironment.Promise;
import org.reactivestreams.tck.TestEnvironment.TestSubscriber;
import org.reactivestreams.tck.support.Optional;
import org.testng.SkipException;
import org.testng.annotations.Test;

public abstract class PublisherVerification<T> {

  private final TestEnvironment env;
  private final long publisherShutdownTimeoutMillis;

  public PublisherVerification(TestEnvironment env, long publisherShutdownTimeoutMillis) {
    this.env = env;
    this.publisherShutdownTimeoutMillis = publisherShutdownTimeoutMillis;
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If `elements` is zero the produced stream must be infinite.
   */
  public abstract Publisher<T> createPublisher(int elements);

  /**
   * Return a Publisher in {@code completed} state in order to run additional tests on it,
   * or {@code null} in order to skip them.
   */
  public abstract Publisher<T> createCompletedStatePublisher();

  /**
   * Return a Publisher in {@code error} state in order to run additional tests on it,
   * or {@code null} in order to skip them.
   */
  public abstract Publisher<T> createErrorStatePublisher();

  ////////////////////// TEST SETUP VERIFICATION ///////////////////////////

  @Test
  public void createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        TestEnvironment.ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced no elements", pub));
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced only 1 element", pub));
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced only 2 elements", pub));
        sub.requestEndOfStream();
      }

      Optional<T> requestNextElementOrEndOfStream(Publisher<T> pub, TestEnvironment.ManualSubscriber<T> sub) throws InterruptedException {
        return sub.requestNextElementOrEndOfStream("Timeout while waiting for next element from Publisher" + pub);
      }

    });
  }


  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////

  // Publisher::subscribe(Subscriber)
  //   when Publisher is in `completed` state
  //     must not call `onSubscribe` on the given Subscriber
  //     must trigger a call to `onComplete` on the given Subscriber
  @Test
  public void publisherSubscribeWhenCompletedMustTriggerOnCompleteAndNotOnSubscribe() throws Throwable {
    completedPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(final Publisher<T> pub) throws InterruptedException {
        final Latch latch = new Latch(env);
        pub.subscribe(
            new TestEnvironment.TestSubscriber<T>(env) {
              public void onComplete() {
                latch.assertOpen(String.format("Publisher %s called `onComplete` twice on new Subscriber", pub));
                latch.close();
              }

              public void onSubscribe(Subscription subscription) {
                env.flop(String.format("Publisher created by `createCompletedStatePublisher()` (%s) called `onSubscribe` on new Subscriber", pub));
              }
            });

        latch.expectClose(env.defaultTimeoutMillis(), String.format("Publisher created by `createPublisher(0)` (%s) did not call `onComplete` on new Subscriber", pub));
        Thread.sleep(env.defaultTimeoutMillis()); // wait for the Publisher to potentially call 'onSubscribe' or `onNext` which would trigger an async error
      }
    });
  }

  // Publisher::subscribe(Subscriber)
  //   when Publisher is in `error` state
  //     must not call `onSubscribe` on the given Subscriber
  //     must trigger a call to `onError` on the given Subscriber
  @Test
  public void publisherSubscribeWhenInErrorStateMustTriggerOnErrorAndNotOnSubscribe() throws Throwable {
    errorPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(final Publisher<T> pub) throws InterruptedException {
        final Latch latch = new Latch(env);
        pub.subscribe(
            new TestEnvironment.TestSubscriber<T>(env) {
              public void onError(Throwable cause) {
                latch.assertOpen(String.format("Error-state Publisher %s called `onError` twice on new Subscriber", pub));
                latch.close();
              }
            });

        latch.expectClose(env.defaultTimeoutMillis(), String.format("Error-state Publisher %s did not call `onError` on new Subscriber", pub));
        Thread.sleep(env.defaultTimeoutMillis()); // wait for the Publisher to potentially call 'onSubscribe' or `onNext` which would trigger an async error

      }
    });
  }

  // Publisher::subscribe(Subscriber)
  //   when Publisher is in `shut-down` state
  //     must not call `onSubscribe` on the given Subscriber
  //     must trigger a call to `onError` with a `java.lang.IllegalStateException` on the given Subscriber
  // Subscription::cancel
  //   when Subscription is not cancelled
  //     the Publisher must shut itself down if the given Subscription is the last downstream Subscription
  @Test
  public void publisherSubscribeWhenInShutDownStateMustTriggerOnErrorAndNotOnSubscribe() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(final Publisher<T> pub) throws InterruptedException {
        TestEnvironment.ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.cancel();

        // we cannot meaningfully test whether the publisher has really shut down
        // however, we can tests whether it reacts to new subscription requests with `onError`
        // after a while
        Thread.sleep(publisherShutdownTimeoutMillis);

        final Latch latch = new Latch(env);
        pub.subscribe(
            new TestEnvironment.TestSubscriber<T>(env) {
              public void onError(Throwable cause) {
                latch.assertOpen(String.format("shut-down-state Publisher %s called `onError` twice on new Subscriber", pub));
                latch.close();
              }
            });
        latch.expectClose(env.defaultTimeoutMillis(), String.format("shut-down-state Publisher %s did not call `onError` on new Subscriber", pub));
        Thread.sleep(env.defaultTimeoutMillis());// wait for the Publisher to potentially call 'onSubscribe' or `onNext` which would trigger an async error
      }
    });
  }

  // Publisher::subscribe(Subscriber)
  //   when Publisher is neither in `completed` nor `error` state
  //     must trigger a call to `onSubscribe` on the given Subscriber if the Subscription is to be accepted
  @Test
  public void publisherSubscribeWhenActiveMustCallOnSubscribeFirst() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        final Latch latch = new Latch(env);
        final Subscription[] sub = {null};
        pub.subscribe(
            new TestSubscriber<T>(env) {
              public void onSubscribe(Subscription subscription) {
                latch.close();
                sub[0] = subscription;
              }
            });

        latch.expectClose(env.defaultTimeoutMillis(), String.format("Active Publisher %s did not call `onSubscribe` on new subscription request", pub));
        sub[0].cancel();
      }
    });
  }

  // Publisher::subscribe(Subscriber)
  //   when Publisher is neither in `completed` nor `error` state
  //     must trigger a call to `onError` on the given Subscriber if the Subscription is to be rejected
  //     must reject the Subscription if the same Subscriber already has an active Subscription
  @Test
  public void publisherSubscribeWhenActiveMustRejectDoubleSubscription() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        final Latch latch = new Latch(env);
        final Promise<Throwable> errorCause = new Promise<Throwable>(env);
        TestSubscriber<T> sub = new TestSubscriber<T>(env) {
          public void onSubscribe(Subscription subscription) { latch.close(); }
          public void onError(Throwable cause) { errorCause.complete(cause); }
        };
        pub.subscribe(sub);
        latch.expectClose(env.defaultTimeoutMillis(), "Active Publisher "+ pub+" did not call `onSubscribe` on first subscription request");
        errorCause.assertUncompleted("Active Publisher "+ pub+" unexpectedly called `onError` on first subscription request");

        latch.reOpen();
        pub.subscribe(sub);
        errorCause.expectCompletion(env.defaultTimeoutMillis(), "Active Publisher "+ pub+" did not call `onError` on double subscription request");
        if(!IllegalStateException.class.isInstance(errorCause.value()))
          env.flop("Publisher " + pub + " called `onError` with " + errorCause.value() + " rather than an `IllegalStateException` on double subscription request");
        latch.assertOpen("Active Publisher "+ pub+" unexpectedly called `onSubscribe` on double subscription request");

      }
    });
  }

  // Subscription::request(Int)
  //   when Subscription is cancelled
  //     must ignore the call
  @Test
  public void subscriptionRequestMoreWhenCancelledMustIgnoreTheCall() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
              sub.subscription.value().cancel();
              sub.subscription.value().request(1); // must not throw
      }
    });
    }

  // Subscription::request(Int)
  //   when Subscription is not cancelled
  //     must register the given number of additional elements to be produced to the respective subscriber
  // A Publisher
  //   must not call `onNext`
  //    more times than the total number of elements that was previously requested with Subscription::request by the corresponding subscriber
  @Test
  public void subscriptionRequestMoreMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
    activePublisherTest(5, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {

        ManualSubscriber<T> sub = env.newManualSubscriber(pub);

        sub.expectNone("Publisher " + pub + " produced value before the first `request`: ");
        sub.request(1);
        sub.nextElement("Publisher " + pub + " produced no element after first `request`");
        sub.expectNone("Publisher " + pub + " produced unrequested: ");

        sub.request(1);
        sub.request(2);
        sub.nextElements(3, env.defaultTimeoutMillis(), "Publisher " + pub + " produced less than 3 elements after two respective `request` calls");

        sub.expectNone("Publisher " + pub + "produced unrequested ");
      }
    });
  }

  // Subscription::request(Int)
  //   when Subscription is not cancelled
  //     must throw a `java.lang.IllegalArgumentException` if the argument is <= 0
  @Test
  public void subscriptionRequestMoreMustThrowIfArgumentIsNonPositive() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {

        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        env.expectThrowingOf(
            IllegalArgumentException.class,
            "Calling `request(-1)` a subscription to " + pub + " did not fail with an `IllegalArgumentException`",
            new Runnable() {
              @Override
              public void run() {
                sub.subscription.value().request(-1);
              }
            });

        env.expectThrowingOf(
            IllegalArgumentException.class,
            "Calling `request(0)` a subscription to " + pub + " did not fail with an `IllegalArgumentException`",
            new Runnable() {
              @Override
              public void run() {
                sub.subscription.value().request(0);
              }
            });
        sub.cancel();
      }
    });
  }

  // Subscription::cancel
  //   when Subscription is cancelled
  //     must ignore the call
  @Test
  public void subscriptionCancelWhenCancelledMustIgnoreCall() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.subscription.value().cancel(); // first time must succeed
        sub.subscription.value().cancel(); // the second time must not throw
      }
    });
  }

  // Subscription::cancel
  //   when Subscription is not cancelled
  //     the Publisher must eventually cease to call any methods on the corresponding subscriber
  @Test
  public void onSubscriptionCancelThePublisherMustEventuallyCeaseToCallAnyMethodsOnTheSubscriber() throws Throwable {
    activePublisherTest(0, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        // infinite stream

        final AtomicBoolean drop = new AtomicBoolean(true);
        ManualSubscriber<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          public void onNext(T element) {
            if (!drop.get()) {
              super.onNext(element);
            }
          }
        };
        env.subscribe(pub, sub);
        sub.request(Integer.MAX_VALUE);
        sub.cancel();
        Thread.sleep(env.defaultTimeoutMillis());

        drop.set(false);// "switch on" element collection
        sub.expectNone(env.defaultTimeoutMillis());
      }
    });
  }

  private interface Function<In, Out> {
    public Out apply(In in) throws Exception;
  }

  // Subscription::cancel
  //   when Subscription is not cancelled
  //     the Publisher must eventually drop any references to the corresponding subscriber
  @Test
  public void onSubscriptionCancelThePublisherMustEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
    final ReferenceQueue<ManualSubscriber<T>> queue = new ReferenceQueue<ManualSubscriber<T>>();

    final Function<Publisher<T>, WeakReference<ManualSubscriber<T>>> run = new Function<Publisher<T>, WeakReference<ManualSubscriber<T>>>() {
      @Override
      public WeakReference<ManualSubscriber<T>> apply(Publisher<T> pub) throws Exception {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        WeakReference<ManualSubscriber<T>> ref = new WeakReference<ManualSubscriber<T>>(sub, queue);
        sub.request(1);
        sub.nextElement();
        sub.cancel();
        return ref;
      }
    };

    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Exception {
        WeakReference<ManualSubscriber<T>> ref = run.apply(pub);

        // cancel may be run asynchronously so we add a sleep before running the GC
        // to "resolve" the race
        Thread.sleep(publisherShutdownTimeoutMillis);
        System.gc();

        if (!queue.remove(100).equals(ref)) {
          env.flop("Publisher " + pub + " did not drop reference to test subscriber after subscription cancellation");
        }
      }
    });
  }

  // A Publisher
  //   must not call `onNext`
  //     after having issued an `onComplete` or `onError` call on a subscriber
  @Test
  public void mustNotCallOnNextAfterHavingIssuedAnOnCompleteOrOnErrorCallOnASubscriber() {
    // this is implicitly verified by the test infrastructure
  }

  // A Publisher
  //   must produce the same elements in the same sequence for all its subscribers
  @Test
  public void mustProduceTheSameElementsInTheSameSequenceForAllItsSubscribers() throws Throwable {
    activePublisherTest(5, new PublisherTestRun<T>() {

      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub3 = env.newManualSubscriber(pub);

        sub1.request(1);
        T x1 = sub1.nextElement("Publisher " + pub + " did not produce the requested 1 element on 1st subscriber");
        sub2.request(2);
        List<T> y1 = sub2.nextElements(2, "Publisher " + pub + " did not produce the requested 2 elements on 2nd subscriber");
        sub1.request(1);
        T x2 = sub1.nextElement("Publisher " + pub + " did not produce the requested 1 element on 1st subscriber");
        sub3.request(3);
        List<T> z1 = sub3.nextElements(3, "Publisher " + pub + " did not produce the requested 3 elements on 3rd subscriber");
        sub3.request(1);
        T z2 = sub3.nextElement("Publisher " + pub + " did not produce the requested 1 element on 3rd subscriber");
        sub3.request(1);
        T z3 = sub3.nextElement("Publisher " + pub + " did not produce the requested 1 element on 3rd subscriber");
        sub3.requestEndOfStream("Publisher " + pub + " did not complete the stream as expected on 3rd subscriber");
        sub2.request(3);
        List<T> y2 = sub2.nextElements(3, "Publisher " + pub + " did not produce the requested 3 elements on 2nd subscriber");
        sub2.requestEndOfStream("Publisher " + pub + " did not complete the stream as expected on 2nd subscriber");
        sub1.request(2);
        List<T> x3 = sub1.nextElements(2, "Publisher " + pub + " did not produce the requested 2 elements on 1st subscriber");
        sub1.request(1);
        T x4 = sub1.nextElement("Publisher " + pub + " did not produce the requested 1 element on 1st subscriber");
        sub1.requestEndOfStream("Publisher " + pub + " did not complete the stream as expected on 1st subscriber");

        //noinspection unchecked
        List<T> r = new ArrayList<T>(Arrays.asList(x1, x2));
        r.addAll(x3);
        r.addAll(Collections.singleton(x4));

        List<T> check1 = new ArrayList<T>(y1);
        check1.addAll(y2);

        //noinspection unchecked
        List<T> check2 = new ArrayList<T>(z1);
        check2.add(z2);
        check2.add(z3);

        assertEquals(r, check1, "Publisher " + pub + " did not produce the same element sequence for subscribers 1 and 2");
        assertEquals(r, check2, "Publisher " + pub + " did not produce the same element sequence for subscribers 1 and 3");
      }
    });
    }

  // A Publisher
  //   must support a pending element count up to 2^63-1 (Long.MAX_VALUE) and provide for overflow protection
  @Test
  public void mustSupportAPendingElementCountUpToLongMaxValue() {
    // not really testable without more control over the Publisher,
    // we verify this part of the fanout logic with the IdentityProcessorVerification
  }

  // A Publisher
  //   must call `onComplete` on a subscriber after having produced the final stream element to it
  //   must call `onComplete` on a subscriber at the earliest possible point in time
  @Test
  public void mustCallOnCompleteOnASubscriberAfterHavingProducedTheFinalStreamElementToIt() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.requestNextElement();
        sub.requestNextElement();
        sub.requestNextElement();
        sub.requestEndOfStream("Publisher " + pub + " did not complete the stream immediately after the final element");
        sub.expectNone();
      }
    });
  }

  // A Publisher
  //   must start producing with the oldest still available element for a new subscriber
  @Test
  public void mustStartProducingWithTheOldestStillAvailableElementForASubscriber() {
    // can only be properly tested if we know more about the Producer implementation
    // like buffer size and buffer retention logic
  }

  // A Publisher
  //   must call `onError` on all its subscribers if it encounters a non-recoverable error
  @Test
  public void  mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError() {
    // not really testable without more control over the Publisher,
    // we verify this part of the fanout logic with the IdentityProcessorVerification
  }

  // A Publisher
  //   must not call `onComplete` or `onError` more than once per subscriber
  @Test
  public void mustNotCallOnCompleteOrOnErrorMoreThanOncePerSubscriber() {
    // this is implicitly verified by the test infrastructure
  }

  /////////////////////// ADDITIONAL "COROLLARY" TESTS //////////////////////

  /////////////////////// TEST INFRASTRUCTURE //////////////////////

  public interface PublisherTestRun<T> {
    public void run(Publisher<T> pub) throws Throwable;
  }

  public void activePublisherTest(int elements, PublisherTestRun<T> body) throws Throwable {
    Publisher<T> pub = createPublisher(elements);
    body.run(pub);
    env.verifyNoAsyncErrors();
  }

  public void completedPublisherTest(PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(createCompletedStatePublisher(), body);
  }

  public void errorPublisherTest(PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(createErrorStatePublisher(), body);
  }

  public void potentiallyPendingTest(Publisher<T> pub, PublisherTestRun<T> body) throws Throwable {
    if (pub != null) {
      body.run(pub);
      env.verifyNoAsyncErrors();
    } else throw new SkipException("Skipping, because no Publisher was provided for this type of test");
  }
}