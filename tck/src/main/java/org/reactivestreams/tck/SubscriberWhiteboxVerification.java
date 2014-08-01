package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.ManualPublisher;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.TestEnvironment.Promise;
import org.reactivestreams.tck.TestEnvironment.Receptacle;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.reactivestreams.tck.Annotations.NotVerified;
import static org.reactivestreams.tck.Annotations.Required;
import static org.testng.Assert.assertTrue;

/**
 * Provides tests for verifying {@link org.reactivestreams.Subscriber} and {@link org.reactivestreams.Subscription} specification rules.
 *
 * @see org.reactivestreams.Subscriber
 * @see org.reactivestreams.Subscription
 */
public abstract class SubscriberWhiteboxVerification<T> {

  private final TestEnvironment env;

  protected SubscriberWhiteboxVerification(TestEnvironment env) {
    this.env = env;
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link org.reactivestreams.Subscriber} instance to be subjected to the testing logic.
   *
   * In order to be meaningfully testable your Subscriber must inform the given
   * `WhiteboxSubscriberProbe` of the respective events having been received.
   */
  public abstract Subscriber<T> createSubscriber(WhiteboxSubscriberProbe<T> probe);

  /**
   * Helper method required for generating test elements.
   * It must create a {@link org.reactivestreams.Publisher} for a stream with exactly the given number of elements.
   * <p>
   * It also must treat the following numbers of elements in these specific ways:
   * <ul>
   *   <li>
   *    If {@code elements} is {@code Long.MAX_VALUE} the produced stream must be infinite.
   *   </li>
   *   <li>
   *    If {@code elements} is {@code 0} the {@code Publisher} should signal {@code onComplete} immediatly.
   *    In other words, it should represent a "completed stream".
   *   </li>
   * </ul>
   */
  public abstract Publisher<T> createHelperPublisher(long elements);

  ////////////////////// TEST ENV CLEANUP /////////////////////////////////////

  @BeforeMethod
  public void setUp() throws Exception {
    env.clearAsyncErrors();
  }

  ////////////////////// TEST SETUP VERIFICATION //////////////////////////////

  @Required @Test
  public void exerciseWhiteboxHappyPath() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.1
  @Required @Test
  public void spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.expectRequest();

        stage.signalNext();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.2
  @NotVerified @Test
  public void spec202_shouldAsynchronouslyDispatch() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.3
    @Required @Test
    public void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable {
      subscriberTestWithoutSetup(new TestStageTestRun() {
        @Override
        public void run(WhiteboxTestStage stage) throws Throwable {
          final Subscription subs = new Subscription() {
            @Override
            public void request(long n) {
              Throwable thr = new Throwable();
              for (StackTraceElement stackTraceElement : thr.getStackTrace()) {
                if (stackTraceElement.getMethodName().equals("onComplete")) {
                  env.flop("Subscription::request MUST NOT be called from onComplete!");
                }
              }
            }

            @Override
            public void cancel() {
              Throwable thr = new Throwable();
              for (StackTraceElement stackTraceElement : thr.getStackTrace()) {
                if (stackTraceElement.getMethodName().equals("onComplete")) {
                  env.flop("Subscriber::onComplete MUST NOT call Subscription::cancel");
                }
              }
            }
          };

          stage.probe = stage.createWhiteboxSubscriberProbe(env);
          final Subscriber<T> sub = createSubscriber(stage.probe);

          sub.onSubscribe(subs);
          sub.onComplete();

          env.verifyNoAsyncErrors();
        }
      });
    }

    // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.3
    @Required @Test
    public void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable {
      subscriberTestWithoutSetup(new TestStageTestRun() {
        @Override
        public void run(WhiteboxTestStage stage) throws Throwable {
          final Subscription subs = new Subscription() {
            @Override
            public void request(long n) {
              Throwable thr = new Throwable();
              for (StackTraceElement stackTraceElement : thr.getStackTrace()) {
                if (stackTraceElement.getMethodName().equals("onError")) {
                  env.flop("Subscriber::onError MUST NOT call Subscription::request!");
                }
              }
            }

            @Override
            public void cancel() {
              Throwable thr = new Throwable();
              for (StackTraceElement stackTraceElement : thr.getStackTrace()) {
                if (stackTraceElement.getMethodName().equals("onError")) {
                  env.flop("Subscriber::onError MUST NOT call Subscription::cancel");
                }
              }
            }
          };

          stage.probe = stage.createWhiteboxSubscriberProbe(env);
          final Subscriber<T> sub = createSubscriber(stage.probe);

          sub.onSubscribe(subs);
          sub.onComplete();

          env.verifyNoAsyncErrors();
        }
      });
    }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.4
  @NotVerified @Test
  public void spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.5
  @Required @Test
  public void spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception {
    new WhiteboxTestStage(env) {{
      // try to subscribe another time, if the subscriber calls `probe.registerOnSubscribe` the test will fail
      sub().onSubscribe(
          new Subscription() {
            @Override
            public void request(long elements) {
              env.flop(String.format("Subscriber %s illegally called `subscription.request(%s)`", sub(), elements));
            }

            @Override
            public void cancel() {
              env.flop(String.format("Subscriber %s illegally called `subscription.cancel()`", sub()));
            }
          });

      env.verifyNoAsyncErrors();
    }};
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.6
  @NotVerified @Test
  public void spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.7
  @NotVerified @Test
  public void spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThread() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
    // the same thread part of the clause can be verified but that is not very useful, or is it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.8
  @Required @Test
  public void spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.puppet().signalCancel();

        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.9
  @Required @Test
  public void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.sendCompletion();
        stage.probe.expectCompletion();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.9
  @Required @Test
  public void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.sendCompletion();
        stage.probe.expectCompletion();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.10
  @Required @Test
  public void spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(1);
        stage.puppet().triggerRequest(1);

        Exception ex = new RuntimeException("Test exception");
        stage.sendError(ex);
        stage.probe.expectError(ex);

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.10
  @Required @Test
  public void spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        Exception ex = new RuntimeException("Test exception");
        stage.sendError(ex);
        stage.probe.expectError(ex);
        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.11
  @NotVerified @Test
  public void spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.12
  @Required @Test
  public void spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable {
    subscriberTestWithoutSetup(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.pub = stage.createHelperPublisher(Long.MAX_VALUE);
        stage.tees = env.newManualSubscriber(stage.pub);
        stage.probe = stage.createWhiteboxSubscriberProbe(env);
        stage.subscribe(createSubscriber(stage.probe));
        stage.probe.expectCompletion(env.defaultTimeoutMillis(), String.format("Subscriber %s did not `registerOnSubscribe`", stage.sub()));
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.13
  @NotVerified @Test
  public void spec213_failingOnCompleteInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.14
  @NotVerified @Test
  public void spec214_failingOnErrorInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  ////////////////////// SUBSCRIPTION SPEC RULE VERIFICATION //////////////////

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.1
  @NotVerified @Test
  public void spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.8
  @Required @Test
  public void spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(2);
        stage.probe.expectNext(stage.signalNext());
        stage.probe.expectNext(stage.signalNext());

        stage.probe.expectNone();
        stage.puppet().triggerRequest(3);
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.9
  @Required @Test
  public void spec309_callingRequestZeroMustThrow() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(final WhiteboxTestStage stage) throws Throwable {
        env.expectThrowingOfWithMessage(IllegalArgumentException.class, "3.9", new Runnable() {
          @Override
          public void run() {
            stage.puppet().triggerRequest(Long.MAX_VALUE);
          }
        });
        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.9
  @Required @Test
  public void spec309_callingRequestWithNegativeNumberMustThrow() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(final WhiteboxTestStage stage) throws Throwable {
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

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.10
  @NotVerified @Test
  public void spec310_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.11
  @NotVerified @Test
  public void spec311_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.12
  @Required @Test
  public void spec312_cancelMustRequestThePublisherToEventuallyStopSignaling() throws Throwable {
    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().signalCancel();
        stage.expectCancelling();
        stage.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.14
  @NotVerified @Test
  public void spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.15
  @NotVerified @Test
  public void spec315_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.16
  @NotVerified @Test
  public void spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.17
  @Required @Test
  public void spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
    // TODO please read into this one, not sure about semantics

    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(Long.MAX_VALUE);

        stage.probe.expectNext(stage.signalNext());

        // to avoid error messages during test harness shutdown
        stage.sendCompletion();
        stage.probe.expectCompletion();

        stage.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.17
  @Required @Test
  public void spec317_mustSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
    // TODO please read into this one, not sure about semantics

    subscriberTest(new TestStageTestRun() {
      @Override
      public void run(WhiteboxTestStage stage) throws InterruptedException {
        stage.puppet().triggerRequest(Long.MAX_VALUE - 1);
        stage.puppet().triggerRequest(Long.MAX_VALUE - 1);

        // cumulative pending > Long.MAX_VALUE
        stage.probe.expectErrorWithMessage(IllegalStateException.class, "3.17");
      }
    });
  }


  /////////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  /////////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  abstract class TestStageTestRun {
    public abstract void run(WhiteboxTestStage stage) throws Throwable;
  }

  public void subscriberTest(TestStageTestRun body) throws Throwable {
    WhiteboxTestStage stage = new WhiteboxTestStage(env, true);
    body.run(stage);
  }

  public void subscriberTestWithoutSetup(TestStageTestRun body) throws Throwable {
    WhiteboxTestStage stage = new WhiteboxTestStage(env, false);
    body.run(stage);
  }

  public class WhiteboxTestStage extends ManualPublisher<T> {
    public Publisher<T> pub;
    public ManualSubscriber<T> tees; // gives us access to an infinite stream of T values
    public WhiteboxSubscriberProbe<T> probe;

    public T lastT = null;

    public WhiteboxTestStage(TestEnvironment env) throws InterruptedException {
      this(env, true);
    }

    public WhiteboxTestStage(TestEnvironment env, boolean runDefaultInit) throws InterruptedException {
      super(env);
      if (runDefaultInit) {
        pub = this.createHelperPublisher(Long.MAX_VALUE);
        tees = env.newManualSubscriber(pub);
        probe = new WhiteboxSubscriberProbe<T>(env, subscriber);
        subscribe(createSubscriber(probe));
        probe.puppet.expectCompletion(env.defaultTimeoutMillis(), String.format("Subscriber %s did not `registerOnSubscribe`", sub()));
      }
    }

    public Subscriber<T> sub() {
      return subscriber.value();
    }

    public SubscriberPuppet puppet() {
      return probe.puppet();
    }

    public Publisher<T> createHelperPublisher(long elements) {
      return SubscriberWhiteboxVerification.this.createHelperPublisher(elements);
    }

    public WhiteboxSubscriberProbe<T> createWhiteboxSubscriberProbe(TestEnvironment env) {
      return new WhiteboxSubscriberProbe<T>(env, subscriber);
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
  }

  /**
   * This class is intented to be used as {@code Subscriber} decorator and should be used in {@code pub.subscriber(...)} calls,
   * in order to allow intercepting calls on the underlying {@code Subscriber}.
   * This delegation allows the proxy to implement {@link org.reactivestreams.tck.SubscriberWhiteboxVerification.BlackboxProbe} assertions.
   */
  public static class BlackboxSubscriberProxy<T> extends BlackboxProbe<T> implements Subscriber<T> {

    public BlackboxSubscriberProxy(TestEnvironment env, Subscriber<T> subscriber) {
      super(env, Promise.completed(env, subscriber));
    }

    @Override
    public void onSubscribe(Subscription s) {
      sub().onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
      registerOnNext(t);
      sub().onNext(t);
    }

    @Override
    public void onError(Throwable cause) {
      registerOnError(cause);
      sub().onError(cause);
    }

    @Override
    public void onComplete() {
      registerOnComplete();
      sub().onComplete();
    }
  }

  public static class BlackboxProbe<T> implements SubscriberProbe<T> {
    protected final TestEnvironment env;
    protected final Promise<Subscriber<T>> subscriber;

    protected final Receptacle<T> elements;
    protected final Promise<Throwable> error;

    public BlackboxProbe(TestEnvironment env, Promise<Subscriber<T>> subscriber) {
      this.env = env;
      this.subscriber = subscriber;
      elements = new Receptacle<T>(env);
      error = new Promise<Throwable>(env);
    }

    @Override
    public void registerOnNext(T element) {
      elements.add(element);
    }

    @Override
    public void registerOnComplete() {
      elements.complete();
    }

    @Override
    public void registerOnError(Throwable cause) {
      error.complete(cause);
    }

    public T expectNext() throws InterruptedException {
      return elements.next(env.defaultTimeoutMillis(), String.format("Subscriber %s did not call `registerOnNext(_)`", sub()));
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

    public Subscriber<T> sub() {
      return subscriber.value();
    }

    public void expectCompletion() throws InterruptedException {
      expectCompletion(env.defaultTimeoutMillis());
    }

    public void expectCompletion(long timeoutMillis) throws InterruptedException {
      expectCompletion(timeoutMillis, String.format("Subscriber %s did not call `registerOnComplete()`", sub()));
    }

    public void expectCompletion(long timeoutMillis, String msg) throws InterruptedException {
      elements.expectCompletion(timeoutMillis, msg);
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

    @SuppressWarnings({"unchecked", "ThrowableResultOfMethodCallIgnored"})
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

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
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

  }

  public static class WhiteboxSubscriberProbe<T> extends BlackboxProbe<T> implements SubscriberPuppeteer {
    protected Promise<SubscriberPuppet> puppet;

    public WhiteboxSubscriberProbe(TestEnvironment env, Promise<Subscriber<T>> subscriber) {
      super(env, subscriber);
      puppet = new Promise<SubscriberPuppet>(env);
    }

    @Override
    public SubscriberPuppet puppet() {
      return puppet.value();
    }

    @Override
    public void registerOnSubscribe(SubscriberPuppet p) {
      if (!puppet.isCompleted()) {
        puppet.complete(p);
      } else {
        env.flop(String.format("Subscriber %s illegally accepted a second Subscription", sub()));
      }
    }

  }

  public interface SubscriberPuppeteer {
    SubscriberPuppet puppet();

    /**
     * Must be called by the test subscriber when it has received the `onSubscribe` event.
     */
    void registerOnSubscribe(SubscriberPuppet puppet);
  }

  public interface SubscriberProbe<T> {

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
    void triggerRequest(long elements);

    void signalCancel();
  }

  public void notVerified() {
    throw new SkipException("Not verified using this TCK.");
  }
}