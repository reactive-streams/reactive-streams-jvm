package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.*;
import org.reactivestreams.tck.support.Optional;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.reactivestreams.tck.Annotations.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
  // TODO is currently not used, remove or find test cases which require it
  public abstract Publisher<T> createCompletedStatePublisher();

  /**
   * Return a Publisher in {@code error} state in order to run additional tests on it,
   * or {@code null} in order to skip them.
   */
  public abstract Publisher<T> createErrorStatePublisher();

  /**
   * Override and return lower value if your Publisher is only able to produce a set number of elements.
   * For example, if it is designed to return at-most-one element, return {@code 1} from this method.
   */
  public long maxElementsFromPublisher() {
    // TODO not used yet everywhere it should
    // general idea is to skip tests that we are unable to run on a given publisher (if it can signal less than we need for a test)
    // see: https://github.com/reactive-streams/reactive-streams/issues/87 for details
    return 10000;
  }

  ////////////////////// TEST ENV CLEANUP /////////////////////////////////////

  @BeforeMethod
  public void setUp() throws Exception {
    env.clearAsyncErrors();
  }

  ////////////////////// TEST SETUP VERIFICATION //////////////////////////////

  @Required @Test
  public void createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced no elements", pub));
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced only 1 element", pub));
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced only 2 elements", pub));
        sub.requestEndOfStream();
      }

      Optional<T> requestNextElementOrEndOfStream(Publisher<T> pub, ManualSubscriber<T> sub) throws InterruptedException {
        return sub.requestNextElementOrEndOfStream("Timeout while waiting for next element from Publisher" + pub);
      }

    });
  }


  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////////

  // 1.1
  // The number of onNext signaled by a Publisher to a Subscriber MUST NOT exceed the cumulative demand
  // that has been signaled via that Subscriber’s Subscription
  @Required @Test
  public void spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
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

  // 1.2
  // A Publisher MAY signal less onNext than requested and terminate the Subscription by calling onComplete or onError
  @Required @Test
  public void spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
    final int elements = 3;
    final int requested = 10;

    activePublisherTest(elements, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(requested);
        sub.nextElements(elements);
        sub.expectCompletion();
      }
    });
  }

  // 1.3
  // onSubscribe, onNext, onError and onComplete signaled to a Subscriber MUST be signaled sequentially (no concurrent notifications)
  @NotVerified @Test
  public void spec103_mustSignalOnMethodsSequentially() throws Exception {
    notVerified(); // can we meaningfully verify this?
  }

  // 1.4
  // If a Publisher fails it MUST signal an onError
  @Required @Test
  public void spec104_mustSignalOnErrorWhenFails() throws Throwable {
    errorPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(final Publisher<T> pub) throws InterruptedException {
        final Latch latch = new Latch(env);
        pub.subscribe(new TestEnvironment.TestSubscriber<T>(env) {
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

  // 1.5.a
  // If a Publisher terminates successfully (finite stream) it MUST signal an onComplete
  @Required @Test
  public void spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.requestNextElement();
        sub.requestNextElement();
        sub.requestNextElement();
        sub.requestEndOfStream();
        sub.expectNone();
      }
    });
  }

  // 1.6
  // If a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription MUST be considered canceled
  @NotVerified @Test
  public void spec106_mustConsiderSubscriptionCancelledAgterOnErrorOrOnCompleteHasBeenCalled() throws Throwable {
    notVerified(); // not really testable without more control over the Publisher
  }

  // 1.7.a
  // Once a terminal state has been signaled (onError, [onComplete]) it is REQUIRED that no further signals occur.
  // Situational scenario MAY apply [see 2.13]
  @Required @Test
  public void spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.requestNextElement();
        sub.expectCompletion();

        sub.request(10);
        sub.expectNone();
      }
    });
  }

  // 1.7.b
  // Once a terminal state has been signaled ([onError], onComplete) it is REQUIRED that no further signals occur.
  // Situational scenario MAY apply [see 2.13]
  @NotVerified @Test
  public void spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable {
    // todo we would need a publisher to subscribe properly, and then error - needs new createSubscribeThenError
    // method to be impled by user I think.

    notVerified(); // can we meaningfully test this, without more control over the publisher?
  }

  // 1.8
  // Subscription's which have been canceled SHOULD NOT receive subsequent onError or onComplete signals,
  // but implementations will not be able to strictly guarantee this in all cases due to the intrinsic
  // race condition between actions taken concurrently by Publisher and Subscriber
  @NotVerified @Test
  public void spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable {
    notVerified(); // can we meaningfully test this?
  }

  // 1.9
  // Publisher.subscribe SHOULD NOT throw a non-fatal Throwable.
  // The only legal way to signal failure (or reject a Subscription) is via the onError method.
  // Non-fatal Throwable excludes any non-recoverable exception by the application (e.g. OutOfMemory)
  @NotVerified @Test
  public void spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 1.10
  // Publisher.subscribe MAY be called as many times as wanted but MUST be with a different Subscriber each time [see 2.12].
  //
  // It is RECOMMENDED to reject the Subscription with a java.lang.IllegalStateException
  // if the same Subscriber already has an active Subscription with this Publisher.
  // The cause message MUST include a reference to this rule and/or quote the full rule
  @Additional @Test
  public void spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable {
    optionalActivePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(final Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);

        pub.subscribe(sub);
        sub.expectErrorWithMessage(IllegalStateException.class, "1.10"); // we do require implementations to mention the rule number at the very least
      }
    });
  }

  // 1.11
  // A Publisher MAY support multi-subscribe and choose whether each Subscription is unicast or multicast
  @Additional @Test
  public void spec111_maySupportMultiSubscribe() throws Throwable {
    optionalActivePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);

        env.verifyNoAsyncErrors();
      }
    });
  }

  // 1.12
  // A Publisher MAY reject calls to its subscribe method if it is unable or unwilling to serve them [1].
  // If rejecting it MUST do this by calling onError on the Subscriber passed to Publisher.subscribe instead of calling onSubscribe
  @Additional(implement = "createErrorStatePublisher") @Test
  public void spec112_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe() throws Throwable {
    // TODO was experimenting with `createUnwillingToSubscribePublisher`, do you think it would be worth exposing such one?
    errorPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final Latch onErrorLatch = new Latch(env);
        ManualSubscriberWithSubscriptionSupport<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          public void onError(Throwable cause) {
            onErrorLatch.assertOpen("Only one onError call expected");
            onErrorLatch.close();
          }

          public void onSubscribe(Subscription subs) {
            env.flop("onSubscribe should not be called if Publisher is unable to subscribe a Subscriber");
          }
        };
        pub.subscribe(sub);
        onErrorLatch.assertClosed("Should have received onError");
      }
    });
  }

  // 1.13
  // A Publisher MUST produce the same elements, starting with the oldest element still available,
  // in the same sequence for all its subscribers
  // and MAY produce the stream elements at (temporarily) differing rates to different subscribers
  @Required @Test
  public void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
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

  // 1.13
  // A Publisher MUST produce the same elements, starting with the oldest element still available,
  // in the same sequence for all its subscribers
  // and MAY produce the stream elements at (temporarily) differing rates to different subscribers
  @Required @Test
  public void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub3 = env.newManualSubscriber(pub);

        List<T> received1 = new ArrayList<T>();
        List<T> received2 = new ArrayList<T>();
        List<T> received3 = new ArrayList<T>();

        sub1.request(3);
        sub2.request(3);
        sub3.request(3);

        received1.addAll(sub1.nextElements(3));
        received2.addAll(sub2.nextElements(3));
        received3.addAll(sub3.nextElements(3));

        sub1.nextElementOrEndOfStream();
        sub2.nextElementOrEndOfStream();
        sub3.nextElementOrEndOfStream();

        assertEquals(received1, received2, String.format("Expected elements to be signaled in the same sequence to both subscribers"));
        assertEquals(received2, received3, String.format("Expected elements to be signaled in the same sequence to both subscribers"));
      }
    });
  }

  ///////////////////// SUBSCRIPTION TESTS //////////////////////////////////

  // 3.13
  // While the Subscription is not cancelled, Subscription.cancel() MUST request the Publisher to eventually drop any
  // references to the corresponding subscriber.
  //
  // Re-subscribing with the same Subscriber object is discouraged [see 2.12], but this specification does not mandate
  // that it is disallowed since that would mean having to store previously canceled subscriptions indefinitely
  @Required @Test
  public void spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
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
      public void run(Publisher<T> pub) throws Throwable {
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

  ///////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  ///////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  public interface PublisherTestRun<T> {
    public void run(Publisher<T> pub) throws Throwable;
  }

  /** Test for feature that SHOULD/MUST be implemented, using a live publisher. */
  public void activePublisherTest(int elements, PublisherTestRun<T> body) throws Throwable {
    if (elements > maxElementsFromPublisher())
      throw new SkipException(String.format("Unable to run this test, as required elements nr: %d is higher than supported by given producer: %d", elements, maxElementsFromPublisher()));

    Publisher<T> pub = createPublisher(elements);
    body.run(pub);
    env.verifyNoAsyncErrors();
  }

  /** Test for feature that MAY be implemented. This test will be marked as SKIPPED if it fails. */
  public void optionalActivePublisherTest(int elements, PublisherTestRun<T> body) throws Throwable {
    if (elements > maxElementsFromPublisher())
      throw new SkipException(String.format("Unable to run this test, as required elements nr: %d is higher than supported by given producer: %d", elements, maxElementsFromPublisher()));

    Publisher<T> pub = createPublisher(elements);
    try {
      potentiallyPendingTest(pub, body);
    } catch (Exception ex) {
       notVerified("Skipped because tested publisher does NOT implement this OPTIONAL requirement.");
    }
  }

  /** Additional test for Publisher in completed state */
  public void completedPublisherTest(PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(createCompletedStatePublisher(), body);
  }

  /** Additional test for Publisher in completed state */
  public void errorPublisherTest(PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(createErrorStatePublisher(), body);
  }

  public void potentiallyPendingTest(Publisher<T> pub, PublisherTestRun<T> body) throws Throwable {
    if (pub != null) {
      body.run(pub);
      env.verifyNoAsyncErrors();
    } else {
      throw new SkipException("Skipping, because no Publisher was provided for this type of test");
    }
  }

  public void notVerified() {
    throw new SkipException("Not verified by this TCK.");
  }

  public void notVerified(String message) {
    throw new SkipException(message);
  }

  private interface Function<In, Out> {
    public Out apply(In in) throws Throwable;
  }

}