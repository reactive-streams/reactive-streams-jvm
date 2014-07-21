package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.*;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.reactivestreams.tck.Annotations.*;
import static org.testng.Assert.assertTrue;

public abstract class SubscriberVerification<T> {

  private final TestEnvironment env;

  protected SubscriberVerification(TestEnvironment env) {
    this.env = env;
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new Subscriber instance to be subjected to the testing logic.
   *
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

  ////////////////////// TEST ENV CLEANUP /////////////////////////////////////

  @BeforeMethod
  public void setUp() throws Exception {
    env.clearAsyncErrors();
  }

  ////////////////////// TEST SETUP VERIFICATION //////////////////////////////

  @Required @Test
  public void exerciseHappyPath() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);

        long receivedRequests = stage.expectRequest();

        stage.signalNext();
        stage.probe.expectNext(stage.lastT);

        stage.puppet().triggerRequest(1);
        if (receivedRequests == 1) {
          stage.expectRequest();
        }

        stage.signalNext();
        stage.probe.expectNext(stage.lastT);

        stage.puppet().signalCancel();
        stage.expectCancelling();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////////

  // 2.1
  // A Subscriber MUST signal demand via Subscription.request(long n) to receive onNext signals
  @Required @Test
  public void spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.expectRequest();

        stage.signalNext();
      }
    });
  }

  // 2.2
  // If a Subscriber suspects that its processing of signals will negatively impact its Publisher's responsivity,
  // it is RECOMMENDED that it asynchronously dispatches its signals
  @NotVerified @Test
  public void spec202_shouldAsynchronouslyDispatch() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 2.3.a
  // [Subscriber.onComplete()] and Subscriber.onError(Throwable t) MUST NOT call any methods on the Subscription or the Publisher
  @NotVerified @Test
  public void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 2.3.b
  // Subscriber.onComplete() and [Subscriber.onError(Throwable t)] MUST NOT call any methods on the Subscription or the Publisher
  @NotVerified @Test
  public void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Exception {
    // cannot be meaningfully tested, or can it?
    notVerified();
  }

  // 2.4
  // Subscriber.onComplete() and Subscriber.onError(Throwable t) MUST consider the Subscription cancelled after having received the signal
  @NotVerified @Test
  public void spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 2.5
  // A Subscriber MUST call Subscription.cancel() on the given Subscription after an onSubscribe signal if it
  // already has an active Subscription
  @Required @Test
  public void spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception {
    new TestStage(env) {{
      // try to subscribe another time, if the subscriber calls `probe.registerOnSubscribe` the test will fail
      sub().onSubscribe(
          new Subscription() {
            public void request(long elements) {
              env.flop(String.format("Subscriber %s illegally called `subscription.request(%s)`", sub(), elements));
            }

            public void cancel() {
              env.flop(String.format("Subscriber %s illegally called `subscription.cancel()`", sub()));
            }
          });

      env.verifyNoAsyncErrors();
    }};
  }

  // 2.6
  // A Subscriber MUST call Subscription.cancel() if it is no longer valid to the Publisher without the Publisher
  // having signaled onError or onComplete
  @NotVerified @Test
  public void spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 2.7
  // A Subscriber MUST ensure that all calls on its Subscription take place from the same thread or provide for
  // respective external synchronization
  @NotVerified @Test
  public void spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThread() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
    // the same thread part of the clause can be verified but that is not very useful, or is it?
  }

  // 2.8
  // A Subscriber MUST be prepared to receive one or more onNext signals after having called Subscription.cancel() if
  // there are still requested elements pending [see 3.12].
  // Subscription.cancel() does not guarantee to perform the underlying cleaning operations immediately
  @Required @Test
  public void spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.puppet().signalCancel();

        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // 2.9.a
  // A Subscriber MUST be prepared to receive an onComplete signal [with] or without a preceding Subscription.request(long n) call
  @Required  @Test
  public void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.sendCompletion();
        stage.probe.expectCompletion();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // 2.9.b
  // A Subscriber MUST be prepared to receive an onComplete signal with or [without] a preceding Subscription.request(long n) call
  @Required @Test
  public void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.sendCompletion();
        stage.probe.expectCompletion();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // 2.10.a
  // A Subscriber MUST be prepared to receive an onError signal [with] or without a preceding Subscription.request(long n) call
  @Required @Test
  public void spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);

        Exception ex = new RuntimeException("Test exception");
        stage.sendError(ex);
        stage.probe.expectError(ex);

        env.verifyNoAsyncErrors();
      }
    });
  }

  // 2.10.b
  // A Subscriber MUST be prepared to receive an onError signal with or [without] a preceding Subscription.request(long n) call
  @Required @Test
  public void spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        Exception ex = new RuntimeException("Test exception");
        stage.sendError(ex);
        stage.probe.expectError(ex);
        env.verifyNoAsyncErrors();
      }
    });
  }

  // 2.11
  // A Subscriber MUST make sure that all calls on its onXXX methods happen-before [1] the processing of the respective signals.
  // I.e. the Subscriber must take care of properly publishing the signal to its processing logic
  @NotVerified @Test
  public void spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 2.12
  // Subscriber.onSubscribe MUST NOT be called more than once (based on object equality)
  @Required @Test
  public void spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable {
    subscriberTestWithoutSetup(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.pub = stage.createHelperPublisher(0);
        stage.tees = env.newManualSubscriber(stage.pub);
        stage.probe = stage.createSubscriberProbe();
        stage.subscribe(createSubscriber(stage.probe));
        stage.probe.expectCompletion(env.defaultTimeoutMillis(), String.format("Subscriber %s did not `registerOnSubscribe`", stage.sub()));
      }
    });
  }

  // 2.13
  // A failing onComplete invocation (e.g. throwing an exception) is a specification violation and MUST
  // signal onError with java.lang.IllegalStateException.
  // The cause message MUST include a reference to this rule and/or quote the full rule
  @NotVerified @Test
  public void spec213_failingOnCompleteInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 2.14
  // A failing onError invocation (e.g. throwing an exception) is a violation of the specification.
  // In this case the Publisher MUST consider a possible Subscription for this Subscriber as canceled.
  // The Publisher MUST raise this error condition in a fashion that is adequate for the runtime environment
  // (e.g. by throwing an exception, notifying a supervisor, logging, etc.).
  @NotVerified @Test
  public void spec214_failingOnErrorInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  ////////////////////// SUBSCRIPTION SPEC RULE VERIFICATION //////////////////

  // 3.1
  // Subscription.request or Subscription.cancel MUST not be called outside of its Subscriber context.
  // A Subscription represents the unique relationship between a Subscriber and a Publisher [see 2.12]
  @NotVerified @Test
  public void spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.2
  // The Subscription MUST allow the Subscriber to call Subscription.request synchronously from within onNext or onSubscribe
  @Required @Test
  public void spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
    subscriberTestWithoutSetup(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.pub = stage.createHelperPublisher(0);
        stage.tees = env.newManualSubscriber(stage.pub);
        stage.probe = stage.createSubscriberProbe();

        stage.subscribe(new ManualSubscriber<T>(env) {
          public void onSubscribe(Subscription subs) {
            this.subscription.complete(subs);

            subs.request(1);
            subs.request(1);
            subs.request(1);
          }

          public void onNext(T element) {
            Subscription subs = this.subscription.value();
            subs.request(1);
            subs.request(1);
            subs.request(1);
          }
        });

        env.verifyNoAsyncErrors();
      }
    });
  }

  // 3.3
  // Subscription.request MUST NOT allow unbounded recursion such as Subscriber.onNext -> Subscription.request -> Subscriber.onNext
  @NotVerified @Test
  public void spec303_mustNotAllowUnboundedRecursion() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
    // notice: could be tested if we knew who's responsibility it is to break the loop.
  }

  // 3.4
  // Subscription.request SHOULD NOT synchronously perform heavy computations that would impact its caller's responsivity
  @NotVerified @Test
  public void spec304_requestShouldNotPerformHeavyComputations() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.5
  // Subscription.cancel MUST NOT synchronously perform heavy computations, MUST be idempotent and MUST be thread-safe
  @NotVerified @Test
  public void spec305_mustNotSynchronouslyPerformHeavyCompuatation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.6
  // After the Subscription is cancelled, additional Subscription.request(long n) MUST be NOPs
  @Required @Test
  public void spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws Exception {
        stage.puppet().signalCancel();
        stage.expectCancelling();

        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);

        stage.probe.expectNone();
      }
    });
  }

  // 3.7
  // After the Subscription is cancelled, additional Subscription.cancel() MUST be NOPs
  @Required @Test
  public void spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws Exception {
        stage.puppet().signalCancel();
        stage.expectCancelling();

        stage.puppet().signalCancel();

        stage.probe.expectNone();
        stage.verifyNoAsyncErrors();
      }
    });
  }

  // 3.8
  // While the Subscription is not cancelled, Subscription.request(long n) MUST register the given number of additional
  // elements to be produced to the respective subscriber
  @Required @Test
  public void spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(2);
        stage.probe.expectNext(stage.signalNext());
        stage.probe.expectNext(stage.signalNext());

        stage.probe.expectNone();
        stage.puppet().triggerRequest(3);
      }
    });
  }

  // 3.9.a
  // While the Subscription is not cancelled, Subscription.request(long n) MUST throw a
  // java.lang.IllegalArgumentException if the argument is <[=] 0.
  // The cause message MUST include a reference to this rule and/or quote the full rule
  @Required @Test
  public void spec309_callingRequestZeroMustThrow() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(final TestStage stage) throws Throwable {
        env.expectThrowingOfWithMessage(IllegalArgumentException.class, "3.9", new Runnable() {
          @Override
          public void run() {
            stage.puppet().triggerRequest(0);
          }
        });
        env.verifyNoAsyncErrors();
      }
    });
  }

  // 3.9.b
  // While the Subscription is not cancelled, Subscription.request(long n) MUST throw a
  // java.lang.IllegalArgumentException if the argument is [<]= 0.
  // The cause message MUST include a reference to this rule and/or quote the full rule
  @Required @Test
  public void spec309_callingRequestWithNegativeNumberMustThrow() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(final TestStage stage) throws Throwable {
        env.expectThrowingOfWithMessage(IllegalArgumentException.class, "3.9", new Runnable() {
          @Override
          public void run() {
            stage.puppet().triggerRequest(-1);
          }
        });
        env.verifyNoAsyncErrors();
      }
    });
  }

  // 3.10
  // While the Subscription is not cancelled, Subscription.request(long n) MAY synchronously call
  // onNext on this (or other) subscriber(s)
  @NotVerified @Test
  public void spec310_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.11
  // While the Subscription is not cancelled, Subscription.request(long n) MAY synchronously call onComplete or onError on this (or other) subscriber(s)
  @NotVerified @Test
  public void spec311_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.12
  // While the Subscription is not cancelled, Subscription.cancel() MUST request the Publisher to eventually stop signaling its Subscriber.
  // The operation is NOT REQUIRED to affect the Subscription immediately.
  @Required @Test
  public void spec312_cancelMustRequestThePublisherToEventuallyStopSignaling() throws Throwable {
    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().signalCancel();
        stage.expectCancelling();
        stage.verifyNoAsyncErrors();
      }
    });
  }

  // 3.14
  // While the Subscription is not cancelled, invoking Subscription.cancel MAY cause the Publisher to transition into
  // the shut-down state if no other Subscription exists at this point [see 1.17].
  @NotVerified @Test
  public void spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.15
  // Subscription.cancel MUST NOT throw an Exception and MUST signal onError to its Subscriber
  @NotVerified @Test
  public void spec315_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.16
  // Subscription.request MUST NOT throw an Exception and MUST signal onError to its Subscriber
  @NotVerified @Test
  public void spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // 3.17.a
  // A `Subscription` MUST support an unbounded number of calls to request and MUST support a pending request count
  // up to `2^63-1` (`java.lang.Long.MAX_VALUE`). A pending request count of exactly `2^63-1` (`java.lang.Long.MAX_VALUE`)
  // MAY be considered by the Publisher as effectively unbounded [1].
  //
  // If more than `2^63-1` are requested in pending then it MUST signal an onError with `java.lang.IllegalStateExceptionon`
  // the given `Subscriber`. The cause message MUST include a reference to this rule and/or quote the full rule.
  @Required @Test
  public void spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
    // TODO please read into this one, not sure about semantics

    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(Long.MAX_VALUE);

          stage.probe.expectNext(stage.signalNext());

        // to avoid error messages during test harness shutdown
        stage.sendCompletion();
        stage.probe.expectCompletion();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // 3.17.b
  // A `Subscription` MUST support an unbounded number of calls to request and MUST support a pending request count
  // up to `2^63-1` (`java.lang.Long.MAX_VALUE`). A pending request count of exactly `2^63-1` (`java.lang.Long.MAX_VALUE`)
  // MAY be considered by the Publisher as effectively unbounded [1].
  //
  // If more than `2^63-1` are requested in pending then it MUST signal an onError with `java.lang.IllegalStateExceptionon`
  // the given `Subscriber`. The cause message MUST include a reference to this rule and/or quote the full rule.
  @Required @Test
  public void spec317_mustSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
    // TODO please read into this one, not sure about semantics

    subscriberTest(new SubscriberTestRun() {
      @Override
      public void run(TestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(Long.MAX_VALUE - 200);
        stage.puppet().triggerRequest(Long.MAX_VALUE - 200);

        // cumulative pending > Long.MAX_VALUE
        stage.probe.expectErrorWithMessage(IllegalStateException.class, "3.17");
      }
    });
  }


  /////////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  /////////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  abstract class SubscriberTestRun {
    public abstract void run(TestStage stage) throws Throwable;
  }

  public void subscriberTest(SubscriberTestRun body) throws Throwable {
    TestStage stage = new TestStage(env, true);
    body.run(stage);
  }

  public void subscriberTestWithoutSetup(SubscriberTestRun body) throws Throwable {
    TestStage stage = new TestStage(env, false);
    body.run(stage);
  }

  public class TestStage extends ManualPublisher<T> {
    Publisher<T> pub;
    ManualSubscriber<T> tees; // gives us access to an infinite stream of T values
    Probe probe;

    T lastT = null;

    public TestStage(TestEnvironment env) throws InterruptedException {
      this(env, true);
    }

    public TestStage(TestEnvironment env, boolean runDefaultInit) throws InterruptedException {
      super(env);
      if (runDefaultInit) {
        pub = this.createHelperPublisher(0);
        tees = env.newManualSubscriber(pub);
        probe = new Probe();
        subscribe(createSubscriber(probe));
        probe.puppet.expectCompletion(env.defaultTimeoutMillis(), String.format("Subscriber %s did not `registerOnSubscribe`", sub()));
      }
    }

    public Subscriber<T> sub() {
      return subscriber.get();
    }

    public Publisher<T> createHelperPublisher(int elements) {
      return SubscriberVerification.this.createHelperPublisher(elements);
    }

    public Probe createSubscriberProbe() {
      return new Probe();
    }

    public SubscriberPuppet puppet() {
      return probe.puppet.value();
    }

    public T signalNext() throws InterruptedException {
      T element = nextT();
      sendNext(element);
      return element;
    }

    public T nextT() throws InterruptedException {
      lastT = tees.requestNextElement();
      return lastT;
    }

    public void verifyNoAsyncErrors() {
      env.verifyNoAsyncErrors();
    }

    public class Probe implements SubscriberProbe<T> {
      Promise<SubscriberPuppet> puppet = new Promise<SubscriberPuppet>(env);
      Receptacle<T> elements = new Receptacle<T>(env);
      Latch completed = new Latch(env);
      Promise<Throwable> error = new Promise<Throwable>(env);

      @Override
      public void registerOnSubscribe(SubscriberPuppet p) {
        if (!puppet.isCompleted()) {
          puppet.complete(p);
        } else {
          env.flop(String.format("Subscriber %s illegally accepted a second Subscription", sub()));
        }
      }

      @Override
      public void registerOnNext(T element) {
        elements.add(element);
      }

      @Override
      public void registerOnComplete() {
        completed.close();
      }

      @Override
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
        expectCompletion(timeoutMillis, String.format("Subscriber %s did not call `registerOnComplete()`", sub()));
      }

      public void expectCompletion(long timeoutMillis, String msg) throws InterruptedException {
        completed.expectClose(timeoutMillis, msg);
      }

      public <E extends Throwable> void expectErrorWithMessage(Class<E> expected, String requiredMessagePart) throws InterruptedException {
        E err = expectError(expected);
        String message = err.getMessage();
        assertTrue(message.contains(requiredMessagePart),
                   String.format("Got expected exception %s but missing message [%s], was: %s", err, expected, requiredMessagePart));
      }
      public <E extends Throwable> E expectError(Class<E> expected) throws InterruptedException {
        return expectError(expected, env.defaultTimeoutMillis());
      }

      public <E extends Throwable> E expectError(Class<E> expected, long timeoutMillis) throws InterruptedException {
        error.expectCompletion(timeoutMillis, String.format("Subscriber %s did not call `registerOnError(%s)`", sub(), expected));
        if (expected.isInstance(error.value())) {
          return (E) error.value();
        } else {
          env.flop(String.format("Subscriber %s called `registerOnError(%s)` rather than `registerOnError(%s)`", sub(), error.value(), expected));

          // make compiler happy
          return null;
        }
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

      public void expectNone() throws InterruptedException {
        expectNone(env.defaultTimeoutMillis());
      }

      public void expectNone(long withinMillis) throws InterruptedException {
        elements.expectNone(withinMillis, "Expected nothing");
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

    void triggerRequest(long elements);

    void signalCancel();
  }

  public void notVerified() {
    throw new SkipException("Not verified using this TCK.");
  }
}