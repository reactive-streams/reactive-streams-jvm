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

/**
 * Provides tests for verifying {@code Publisher} specification rules.
 *
 * @see org.reactivestreams.Publisher
 */
public abstract class PublisherVerification<T> {

  private final TestEnvironment env;
  private final long publisherReferenceGCTimeoutMillis;

  public PublisherVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis) {
    this.env = env;
    this.publisherReferenceGCTimeoutMillis = publisherReferenceGCTimeoutMillis;
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If `elements` is `Long.MAX_VALUE` the produced stream must be infinite.
   */
  public abstract Publisher<T> createPublisher(long elements);

  /**
   * Return a Publisher in {@code error} state in order to run additional tests on it,
   * or {@code null} in order to skip them.
   */
  public abstract Publisher<T> createErrorStatePublisher();

  /**
   * Override and return lower value if your Publisher is only able to produce a finite number of elements.
   * For example, if it is designed to return at-most-one element, return {@code 1} from this method.
   */
  public long maxElementsFromPublisher() {
    // general idea is to skip tests that we are unable to run on a given publisher (if it can signal less than we need for a test)
    // see: https://github.com/reactive-streams/reactive-streams/issues/87 for details
    return Long.MAX_VALUE;
  }

  /**
   * In order to verify rule 3.3 of the reactive streams spec, this number will be used to check if a
   * {@code Subscription} actually solves the "unbounded recursion" problem by not allowing the number of
   * recursive calls to exceed the number returned by this method.
   *
   * @see <a href="https://github.com/reactive-streams/reactive-streams#3.3">reactive streams spec, rule 3.3</a>
   * @see PublisherVerification#spec303_mustNotAllowUnboundedRecursion()
   */
  public long boundedDepthOfOnNextAndRequestRecursion() {
    return 1;
  }

  ////////////////////// TEST ENV CLEANUP /////////////////////////////////////

  @BeforeMethod
  public void setUp() throws Exception {
    env.clearAsyncErrors();
  }

  ////////////////////// TEST SETUP VERIFICATION //////////////////////////////

  @Required @Test
  public void createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced no elements", pub));
        sub.requestEndOfStream();
      }

      Optional<T> requestNextElementOrEndOfStream(Publisher<T> pub, ManualSubscriber<T> sub) throws InterruptedException {
        return sub.requestNextElementOrEndOfStream("Timeout while waiting for next element from Publisher" + pub);
      }

    });
  }

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

  @Required @Test
  public void validate_maxElementsFromPublisher() throws Exception {
    assertTrue(maxElementsFromPublisher() >= 0, "maxElementsFromPublisher MUST return a number >= 0");
  }

  @Required @Test
  public void validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
    assertTrue(boundedDepthOfOnNextAndRequestRecursion() >= 1, "boundedDepthOfOnNextAndRequestRecursion must return a number >= 1");
  }


  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////////

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.1
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.2
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.3
  @Stochastic @Test
  public void spec103_mustSignalOnMethodsSequentially() throws Throwable {
    testRuns(100, new Function<Integer, Void>() {
      @Override
      public Void apply(Integer runNumber) throws Throwable {
        activePublisherTest(10, new PublisherTestRun<T>() {
          @Override
          public void run(Publisher<T> pub) throws Throwable {
            final Latch latch = new Latch(env);

            pub.subscribe(new Subscriber<T>() {
              Subscription subs;

              @Override
              public void onSubscribe(Subscription s) {
                latch.assertOpen("Expected latch to be open during onSubscribe call");
                latch.close();

                subs = s;
                subs.request(1);

                latch.reOpen();
              }

              @Override
              public void onNext(T ignore) {
                latch.assertOpen("Expected latch to be open during onNext call");
                latch.close();

                // ignore value
                subs.request(1);

                latch.reOpen();
              }

              @Override
              public void onError(Throwable t) {
                latch.assertOpen("Expected latch to be open during onError call");
                latch.close();
                // ignore value
                latch.reOpen();
              }

              @Override
              public void onComplete() {
                latch.assertOpen("Expected latch to be open during onComplete call");
                latch.close();
                latch.reOpen();
              }
            });
          }
        });
        return null;
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.4
  @Additional(implement = "createErrorStatePublisher") @Test
  public void spec104_mustSignalOnErrorWhenFails() throws Throwable {
    errorPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(final Publisher<T> pub) throws InterruptedException {
        final Latch latch = new Latch(env);
        pub.subscribe(new TestEnvironment.TestSubscriber<T>(env) {
          @Override
          public void onError(Throwable cause) {
            latch.assertOpen(String.format("Error-state Publisher %s called `onError` twice on new Subscriber", pub));
            latch.close();
          }
        });

        latch.expectClose(env.defaultTimeoutMillis(), String.format("Error-state Publisher %s did not call `onError` on new Subscriber", pub));
        Thread.sleep(env.defaultTimeoutMillis()); // wait for the Publisher to potentially call 'onSubscribe' or `onNext` which would trigger an async error

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.5
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.6
  @NotVerified @Test
  public void spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable {
    notVerified(); // not really testable without more control over the Publisher
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.7
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.7
  @NotVerified @Test
  public void spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable {
    // todo we would need a publisher to subscribe properly, and then error - needs new createSubscribeThenError
    // method to be implemented by user I think.

    notVerified(); // can we meaningfully test this, without more control over the publisher?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.8
  @NotVerified @Test
  public void spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable {
    notVerified(); // can we meaningfully test this?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.9
  @NotVerified @Test
  public void spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.10
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.11
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.12
  @Additional(implement = "createErrorStatePublisher") @Test
  public void spec112_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe() throws Throwable {
    errorPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final Latch onErrorLatch = new Latch(env);
        ManualSubscriberWithSubscriptionSupport<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          @Override
          public void onError(Throwable cause) {
            onErrorLatch.assertOpen("Only one onError call expected");
            onErrorLatch.close();
          }

          @Override
          public void onSubscribe(Subscription subs) {
            env.flop("onSubscribe should not be called if Publisher is unable to subscribe a Subscriber");
          }
        };
        pub.subscribe(sub);
        onErrorLatch.assertClosed("Should have received onError");

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.13
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

        @SuppressWarnings("unchecked")
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.13
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

        sub1.expectCompletion();
        sub2.expectCompletion();
        sub3.expectCompletion();

        assertEquals(received1, received2, String.format("Expected elements to be signaled in the same sequence to 1st and 2nd subscribers"));
        assertEquals(received2, received3, String.format("Expected elements to be signaled in the same sequence to 2nd and 3rd subscribers"));
      }
    });
  }

  ///////////////////// SUBSCRIPTION TESTS //////////////////////////////////

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.2
  @Required @Test
  public void spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
    activePublisherTest(6, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = new ManualSubscriber<T>(env) {
          @Override
          public void onSubscribe(Subscription subs) {
            this.subscription.complete(subs);

            subs.request(1);
            subs.request(1);
            subs.request(1);
          }

          @Override
          public void onNext(T element) {
            Subscription subs = this.subscription.value();
            subs.request(1);
          }
        };

        env.subscribe(pub, sub);

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.3
  @Required @Test
  @Additional(implement = "boundedDepthOfOnNextAndRequestRecursion")
  public void spec303_mustNotAllowUnboundedRecursion() throws Throwable {
    long oneMoreThanBoundedLimit = boundedDepthOfOnNextAndRequestRecursion() + 1;

    activePublisherTest(oneMoreThanBoundedLimit, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ThreadLocal<Long> stackDepthCounter = new ThreadLocal<Long>(){
          @Override
          protected Long initialValue() {
            return 0L;
          }
        };

        final ManualSubscriber<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          @Override
          public void onNext(T element) {
            stackDepthCounter.set(stackDepthCounter.get() + 1);
            super.onNext(element);

            final Long callsUntilNow = stackDepthCounter.get();
            if (callsUntilNow > boundedDepthOfOnNextAndRequestRecursion()) {
              env.flop(String.format("Got %d onNext calls within thread: %s, yet expected recursive bound was %d",
                                     callsUntilNow, Thread.currentThread(), boundedDepthOfOnNextAndRequestRecursion()));
            }

            // request more right away, the Publisher must break the recursion
            subscription.value().request(1);

            stackDepthCounter.set(stackDepthCounter.get() - 1);
          }
        };

        env.subscribe(pub, sub);

        sub.request(1); // kick-off the `request -> onNext -> request -> onNext -> ...`

        sub.nextElementOrEndOfStream();
        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.4
  @NotVerified @Test
  public void spec304_requestShouldNotPerformHeavyComputations() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.5
  @NotVerified @Test
  public void spec305_cancelMustNotSynchronouslyPerformHeavyCompuatation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.6
  @Required @Test
  public void spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {

        // override ManualSubscriberWithSubscriptionSupport#cancel because by default a ManualSubscriber will drop the
        // subscription once it's cancelled (as expected).
        // In this test however it must keep the cancelled Subscription and keep issuing `request(long)` to it.
        ManualSubscriber<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          @Override
          public void cancel() {
            if (subscription.isCompleted()) {
              subscription.value().cancel();
            } else {
              env.flop("Cannot cancel a subscription before having received it");
            }
          }
        };

        env.subscribe(pub, sub);

        sub.cancel();
        sub.request(1);
        sub.request(1);
        sub.request(1);

        sub.expectNone();
        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.7
  @Required @Test
  public void spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable {
    activePublisherTest(1, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);

        // leak the Subscription
        final Subscription subs = sub.subscription.value();

        subs.cancel();
        subs.cancel();
        subs.cancel();

        sub.expectNone();
        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.13
  @Required @Test
  public void spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
    final ReferenceQueue<ManualSubscriber<T>> queue = new ReferenceQueue<ManualSubscriber<T>>();

    final Function<Publisher<T>, WeakReference<ManualSubscriber<T>>> run = new Function<Publisher<T>, WeakReference<ManualSubscriber<T>>>() {
      @Override
      public WeakReference<ManualSubscriber<T>> apply(Publisher<T> pub) throws Exception {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        final WeakReference<ManualSubscriber<T>> ref = new WeakReference<ManualSubscriber<T>>(sub, queue);

        sub.request(1);
        sub.nextElement();
        sub.cancel();

        return ref;
      }
    };

    activePublisherTest(3, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final WeakReference<ManualSubscriber<T>> ref = run.apply(pub);

        // cancel may be run asynchronously so we add a sleep before running the GC
        // to "resolve" the race
        Thread.sleep(publisherReferenceGCTimeoutMillis);
        System.gc();

        if (!queue.remove(100).equals(ref)) {
          env.flop("Publisher " + pub + " did not drop reference to test subscriber after subscription cancellation");
        }
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.17
  @Required @Test
  public void spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
    final int totalElements = 3;

    activePublisherTest(totalElements, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(Long.MAX_VALUE);

        sub.nextElements(totalElements);
        sub.expectCompletion();

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.17
  @Required @Test
  public void spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable {
    final int totalElements = 3;

    activePublisherTest(totalElements, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(Long.MAX_VALUE / 2); // pending = Long.MAX_VALUE / 2
        sub.request(Long.MAX_VALUE / 2); // pending = Long.MAX_VALUE - 1
        sub.request(1); // pending = Long.MAX_VALUE

        sub.nextElements(totalElements);
        sub.expectCompletion();

        env.verifyNoAsyncErrors();
      }
    });
  }

  ///////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  ///////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  public interface PublisherTestRun<T> {
    public void run(Publisher<T> pub) throws Throwable;
  }

  /**
   * Test for feature that SHOULD/MUST be implemented, using a live publisher.
   */
  public void activePublisherTest(long elements, PublisherTestRun<T> body) throws Throwable {
    if (elements > maxElementsFromPublisher()) {
      throw new SkipException(String.format("Unable to run this test, as required elements nr: %d is higher than supported by given producer: %d", elements, maxElementsFromPublisher()));
    }

    Publisher<T> pub = createPublisher(elements);
    body.run(pub);
    env.verifyNoAsyncErrors();
  }

  /**
   * Test for feature that MAY be implemented. This test will be marked as SKIPPED if it fails.
   */
  public void optionalActivePublisherTest(long elements, PublisherTestRun<T> body) throws Throwable {
    if (elements > maxElementsFromPublisher()) {
      throw new SkipException(String.format("Unable to run this test, as required elements nr: %d is higher than supported by given producer: %d", elements, maxElementsFromPublisher()));
    }

    Publisher<T> pub = createPublisher(elements);
    try {
      potentiallyPendingTest(pub, body);
    } catch (Exception ex) {
      notVerified("Skipped because tested publisher does NOT implement this OPTIONAL requirement.");
    }
  }

  /**
   * Additional test for Publisher in error state
   */
  public void errorPublisherTest(PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(createErrorStatePublisher(), body);
  }

  public void potentiallyPendingTest(Publisher<T> pub, PublisherTestRun<T> body) throws Throwable {
    if (pub != null) {
      body.run(pub);
    } else {
      throw new SkipException("Skipping, because no Publisher was provided for this type of test");
    }
  }

  public void testRuns(int n, Function<Integer, Void> body) throws Throwable {
    for (int i = 0; i < n; i++) {
      body.apply(i);
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