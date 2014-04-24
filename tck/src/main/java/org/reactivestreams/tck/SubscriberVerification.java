package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.Latch;
import org.reactivestreams.tck.TestEnvironment.ManualPublisher;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.TestEnvironment.Promise;
import org.reactivestreams.tck.TestEnvironment.Receptacle;
import org.testng.annotations.Test;

public abstract class SubscriberVerification<T> {

  private final TestEnvironment env;

  protected SubscriberVerification(TestEnvironment env) {
    this.env = env;
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new Subscriber instance to be subjected to the testing logic.
   * <p/>
   * In order to be meaningfully testable your Subscriber must inform the given
   * `SubscriberProbe` of the respective events having been received.
   */
  public abstract Subscriber<T> createSubscriber(SubscriberProbe<T> probe);

  /**
   * Helper method required for generating test elements.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If `elements` is zero the produced stream must be infinite.
   */
  public abstract Publisher<T> createHelperPublisher(int elements);

  ////////////////////// TEST SETUP VERIFICATION ///////////////////////////

  @Test
  public void exerciseHappyPath() throws InterruptedException {
    new TestSetup(env) {{
      puppet().triggerRequestMore(1);

      puppet().triggerRequestMore(1);
      int receivedRequests = expectRequestMore();
      sendNextTFromUpstream();
      probe.expectNext(lastT);

      puppet().triggerRequestMore(1);
      if (receivedRequests == 1) {
        expectRequestMore();
      }
      sendNextTFromUpstream();
      probe.expectNext(lastT);

      puppet().triggerCancel();
      expectCancelling();

      env.verifyNoAsyncErrors();
    }};
  }

  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////

  // Subscriber::onSubscribe(Subscription), Subscriber::onNext(T)
  //   must asynchronously schedule a respective event to the subscriber
  //   must not call any methods on the Subscription, the Publisher or any other Publishers or Subscribers
  @Test
  public void onSubscribeAndOnNextMustAsynchronouslyScheduleAnEvent() {
    // cannot be meaningfully tested, or can it?
  }

  // Subscriber::onComplete, Subscriber::onError(Throwable)
  //   must asynchronously schedule a respective event to the Subscriber
  //   must not call any methods on the Subscription, the Publisher or any other Publishers or Subscribers
  //   must consider the Subscription cancelled after having received the event
  @Test
  public void onCompleteAndOnErrorMustAsynchronouslyScheduleAnEvent() {
    // cannot be meaningfully tested, or can it?
  }

  // A Subscriber
  //   must not accept an `onSubscribe` event if it already has an active Subscription
  @Test
  public void mustNotAcceptAnOnSubscribeEventIfItAlreadyHasAnActiveSubscription() throws InterruptedException {
    new TestSetup(env) {{
      // try to subscribe another time, if the subscriber calls `probe.registerOnSubscribe` the test will fail
      sub().onSubscribe(
          new Subscription() {
            public void request(int elements) {
              env.flop(String.format("Subscriber %s illegally called `subscription.requestMore(%s)`", sub(), elements));
            }

            public void cancel() {
              env.flop(String.format("Subscriber %s illegally called `subscription.cancel()`", sub()));
            }
          });

      env.verifyNoAsyncErrors();
      }};
  }

  // A Subscriber
  //   must call Subscription::cancel during shutdown if it still has an active Subscription
  @Test
  public void mustCallSubscriptionCancelDuringShutdownIfItStillHasAnActiveSubscription() throws InterruptedException {
    new TestSetup(env) {{
      puppet().triggerShutdown();
      expectCancelling();

      env.verifyNoAsyncErrors();
    }};
  }

  // A Subscriber
  //   must ensure that all calls on a Subscription take place from the same thread or provide for respective external synchronization
  @Test
  public void mustEnsureThatAllCallsOnASubscriptionTakePlaceFromTheSameThreadOrProvideExternalSync() {
    // cannot be meaningfully tested, or can it?
  }

  // A Subscriber
  //   must be prepared to receive one or more `onNext` events after having called Subscription::cancel
  @Test
  public void mustBePreparedToReceiveOneOrMoreOnNextEventsAfterHavingCalledSubscriptionCancel() throws InterruptedException {
    new TestSetup(env) {{
      puppet().triggerRequestMore(1);
      puppet().triggerCancel();
      expectCancelling();
      sendNextTFromUpstream();

      env.verifyNoAsyncErrors();
    }};
  }

  // A Subscriber
  //   must be prepared to receive an `onComplete` event with a preceding Subscription::requestMore call
  @Test
  public void mustBePreparedToReceiveAnOnCompleteEventWithAPrecedingSubscriptionRequestMore() throws InterruptedException {
    new TestSetup(env) {{
      puppet().triggerRequestMore(1);
      sendCompletion();
      probe.expectCompletion();

      env.verifyNoAsyncErrors();
    }};
  }

  // A Subscriber
  //   must be prepared to receive an `onComplete` event without a preceding Subscription::requestMore call
  @Test
  public void mustBePreparedToReceiveAnOnCompleteEventWithoutAPrecedingSubscriptionRequestMore() throws InterruptedException {
    new TestSetup(env) {{
      sendCompletion();
      probe.expectCompletion();

      env.verifyNoAsyncErrors();
    }};
  }

  // A Subscriber
  //   must be prepared to receive an `onError` event with a preceding Subscription::requestMore call
  @Test
  public void mustBePreparedToReceiveAnOnErrorEventWithAPrecedingSubscriptionRequestMore() throws InterruptedException {
    new TestSetup(env) {{
      puppet().triggerRequestMore(1);
      Exception ex = new RuntimeException("Test exception");
      sendError(ex);
      probe.expectError(ex);

      env.verifyNoAsyncErrors();
    }};
  }

  // A Subscriber
  //   must be prepared to receive an `onError` event without a preceding Subscription::requestMore call
  @Test
  public void mustBePreparedToReceiveAnOnErrorEventWithoutAPrecedingSubscriptionRequestMore() throws InterruptedException {
    new TestSetup(env) {{
      Exception ex = new RuntimeException("Test exception");
      sendError(ex);
      probe.expectError(ex);
      env.verifyNoAsyncErrors();
    }};
  }

  // A Subscriber
  //   must make sure that all calls on its `onXXX` methods happen-before the processing of the respective events
  @Test
  public void mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() {
    // cannot be meaningfully tested, or can it?
  }

  /////////////////////// ADDITIONAL "COROLLARY" TESTS //////////////////////

  /////////////////////// TEST INFRASTRUCTURE //////////////////////

  public class TestSetup extends ManualPublisher<T> {
    ManualSubscriber<T> tees; // gives us access to an infinite stream of T values
    Probe probe;
    T lastT = null;

    public TestSetup(TestEnvironment env) throws InterruptedException {
      super(env);
      tees = env.newManualSubscriber(createHelperPublisher(0));
      probe = new Probe();
      subscribe(createSubscriber(probe));
      probe.puppet.expectCompletion(env.defaultTimeoutMillis(), String.format("Subscriber %s did not `registerOnSubscribe`", sub()));
    }

    public Subscriber<T> sub() {
      return subscriber.get();
    }

    public SubscriberPuppet puppet() {
      return probe.puppet.value();
    }

    public void sendNextTFromUpstream() throws InterruptedException {
      sendNext(nextT());
    }

    public T nextT() throws InterruptedException {
      lastT = tees.requestNextElement();
      return lastT;
    }

    public class Probe implements SubscriberProbe<T> {
      Promise<SubscriberPuppet> puppet = new Promise<SubscriberPuppet>(env);
      Receptacle<T> elements = new Receptacle<T>(env);
      Latch completed = new Latch(env);
      Promise<Throwable> error = new Promise<Throwable>(env);

      public void registerOnSubscribe(SubscriberPuppet p) {
        if (!puppet.isCompleted()) {
          puppet.complete(p);
        } else {
          env.flop(String.format("Subscriber %s illegally accepted a second Subscription", sub()));
        }
      }

      public void registerOnNext(T element) {
        elements.add(element);
      }

      public void registerOnComplete() {
        completed.close();
      }

      public void registerOnError(Throwable cause) {
        error.complete(cause);
      }

      public void expectNext(T expected) throws InterruptedException {
        expectNext(expected, env.defaultTimeoutMillis());
      }

      public void expectNext(T expected, long timeoutMillis) throws InterruptedException {
        T received = elements.next(timeoutMillis, String.format("Subscriber %s did not call `registerOnNext(%s)`", sub(), expected));
        if (!received.equals(expected)) {
          env.flop(String.format("Subscriber %s called `registerOnNext(%s)` rather than `registerOnNext(%s)`", sub(), received, expected));
        }
      }

      public void expectCompletion() throws InterruptedException {
        expectCompletion(env.defaultTimeoutMillis());
      }

      public void expectCompletion(long timeoutMillis) throws InterruptedException {
        completed.expectClose(timeoutMillis, String.format("Subscriber %s did not call `registerOnComplete()`", sub()));
      }

      public void expectError(Throwable expected) throws InterruptedException {
        expectError(expected, env.defaultTimeoutMillis());
      }

      public void expectError(Throwable expected, long timeoutMillis) throws InterruptedException {
        error.expectCompletion(timeoutMillis, String.format("Subscriber %s did not call `registerOnError(%s)`", sub(), expected));
        if (error.value() != expected) {
          env.flop(String.format("Subscriber %s called `registerOnError(%s)` rather than `registerOnError(%s)`", sub(), error.value(), expected));
        }
      }

      public void verifyNoAsyncErrors() {
        env.verifyNoAsyncErrors();
      }
    }
  }

  public interface SubscriberProbe<T> {
    /**
     * Must be called by the test subscriber when it has received the `onSubscribe` event.
     */
    void registerOnSubscribe(SubscriberPuppet puppet);

    /**
     * Must be called by the test subscriber when it has received an`onNext` event.
     */
    void registerOnNext(T element);

    /**
     * Must be called by the test subscriber when it has received an `onComplete` event.
     */
    void registerOnComplete();

    /**
     * Must be called by the test subscriber when it has received an `onError` event.
     */
    void registerOnError(Throwable cause);
  }

  public interface SubscriberPuppet {
    void triggerShutdown();

    void triggerRequestMore(int elements);

    void triggerCancel();
  }
}