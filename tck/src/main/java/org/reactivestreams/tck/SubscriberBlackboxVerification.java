package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.ManualPublisher;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.support.Optional;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.reactivestreams.tck.Annotations.NotVerified;
import static org.reactivestreams.tck.Annotations.Required;
import static org.reactivestreams.tck.SubscriberWhiteboxVerification.BlackboxSubscriberProxy;

/**
 * Provides tests for verifying {@link org.reactivestreams.Subscriber} and {@link org.reactivestreams.Subscription}
 * specification rules, without any modifications to the tested implementation (also known as "Black Box" testing).
 *
 * This verification is NOT able to check many of the rules of the spec, and if you want more
 * verification of your implementation you'll have to implement {@code org.reactivestreams.tck.SubscriberWhiteboxVerification}
 * instead.
 *
 * @see org.reactivestreams.Subscriber
 * @see org.reactivestreams.Subscription
 */
public abstract class SubscriberBlackboxVerification<T> {

  private final TestEnvironment env;

  protected SubscriberBlackboxVerification(TestEnvironment env) {
    this.env = env;
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link org.reactivestreams.Subscriber} instance to be subjected to the testing logic.
   */
  public abstract Subscriber<T> createSubscriber();

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

  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////////

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.1
  @Required @Test
  public void spec201_blackbox_mustSignalDemandViaSubscriptionRequest() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws InterruptedException {
        final long n = stage.expectRequest();// assuming subscriber wants to consume elements...

        // should cope with up to requested number of elements
        for (int i = 0; i < n; i++)
          stage.signalNext();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.2
  @NotVerified @Test
  public void spec202_blackbox_shouldAsynchronouslyDispatch() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.3
  @Required @Test
  public void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscription subs = new Subscription() {
          @Override
          public void request(long n) {
            final Throwable thr = new Throwable();
            for (StackTraceElement stackElem : thr.getStackTrace()) {
              if (stackElem.getMethodName().equals("onComplete")) {
                env.flop("Subscription::request MUST NOT be called from onComplete!");
              }
            }
          }

          @Override
          public void cancel() {
            final Throwable thr = new Throwable();
            for (StackTraceElement stackElem : thr.getStackTrace()) {
              if (stackElem.getMethodName().equals("onComplete")) {
                env.flop("Subscriber::onComplete MUST NOT call Subscription::cancel");
              }
            }
          }
        };

        final Subscriber<T> sub = createSubscriber();
        sub.onSubscribe(subs);
        sub.onComplete();

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.3
  @Required @Test
  public void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscription subs = new Subscription() {
          @Override
          public void request(long n) {
            final Throwable thr = new Throwable();
            for (StackTraceElement stackElem : thr.getStackTrace()) {
              if (stackElem.getMethodName().equals("onError")) {
                env.flop(String.format("Subscriber::onError MUST NOT call Subscription::request! (Caller: %s::%s line %d)",
                                       stackElem.getClassName(), stackElem.getMethodName(), stackElem.getLineNumber()));
              }
            }
          }

          @Override
          public void cancel() {
            final Throwable thr = new Throwable();
            for (StackTraceElement stackElem : thr.getStackTrace()) {
              if (stackElem.getMethodName().equals("onError")) {
                env.flop(String.format("Subscriber::onError MUST NOT call Subscription::cancel (Caller: %s::%s line %d)",
                                       stackElem.getClassName(), stackElem.getMethodName(), stackElem.getLineNumber()));
              }
            }
          }
        };

        final Subscriber<T> sub = createSubscriber();
        sub.onSubscribe(subs);
        sub.onError(new Throwable("Boom!"));

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.4
  @NotVerified @Test
  public void spec204_blackbox_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.5
  @Required @Test
  public void spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception {
    new BlackboxTestStage(env) {{
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

      env.verifyNoAsyncErrors(env.defaultTimeoutMillis());
    }};
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.6
  @NotVerified @Test
  public void spec206_blackbox_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.7
  @NotVerified @Test
  public void spec207_blackbox_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThread() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
    // the same thread part of the clause can be verified but that is not very useful, or is it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.8
  @NotVerified @Test
  public void spec208_blackbox_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.9
  @Required @Test
  public void spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Publisher<T> pub = createHelperPublisher(0);

        final Subscriber<T> sub = createSubscriber();
        final BlackboxSubscriberProxy<T> probe = stage.createBlackboxSubscriberProxy(env, sub);

        pub.subscribe(probe);
        probe.expectCompletion();
        probe.expectNone();

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.9
  @Required @Test
  public void spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Publisher<T> pub = new Publisher<T>() {
          @Override
          public void subscribe(Subscriber<T> s) {
            s.onComplete();
          }
        };

        final Subscriber<T> sub = createSubscriber();
        final BlackboxSubscriberProxy<T> probe = stage.createBlackboxSubscriberProxy(env, sub);

        pub.subscribe(probe);
        probe.expectCompletion();

        env.verifyNoAsyncErrors();
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.10
  @Required @Test
  public void spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override
      @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      public void run(BlackboxTestStage stage) throws Throwable {
        stage.sub().onError(new Throwable("Boom!"));
        stage.subProxy().expectError(Throwable.class);
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.11
  @NotVerified @Test
  public void spec211_blackbox_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.12
  @Required @Test
  public void spec212_blackbox_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.13
  @NotVerified @Test
  public void spec213_blackbox_failingOnCompleteInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#2.14
  @NotVerified @Test
  public void spec214_blackbox_failingOnErrorInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  ////////////////////// SUBSCRIPTION SPEC RULE VERIFICATION //////////////////

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.1
  @NotVerified @Test
  public void spec301_blackbox_mustNotBeCalledOutsideSubscriberContext() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.8
  @Required @Test
  public void spec308_blackbox_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.9
  @Required @Test
  public void spec309_blackbox_callingRequestZeroMustThrow() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.9
  @Required @Test
  public void spec309_blackbox_callingRequestWithNegativeNumberMustThrow() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.10
  @NotVerified @Test
  public void spec310_blackbox_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.11
  @NotVerified @Test
  public void spec311_blackbox_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.12
  @Required @Test
  public void spec312_blackbox_cancelMustRequestThePublisherToEventuallyStopSignaling() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.14
  @NotVerified @Test
  public void spec314_blackbox_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.15
  @NotVerified @Test
  public void spec315_blackbox_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#3.16
  @NotVerified @Test
  public void spec316_blackbox_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  /////////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  /////////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  abstract class BlackboxTestStageTestRun {
    public abstract void run(BlackboxTestStage stage) throws Throwable;
  }

  public void blackboxSubscriberTest(BlackboxTestStageTestRun body) throws Throwable {
    BlackboxTestStage stage = new BlackboxTestStage(env, true);
    body.run(stage);
  }

  public void blackboxSubscriberWithoutSetupTest(BlackboxTestStageTestRun body) throws Throwable {
    BlackboxTestStage stage = new BlackboxTestStage(env, false);
    body.run(stage);
  }

  public class BlackboxTestStage extends ManualPublisher<T> {
    public Publisher<T> pub;
    public ManualSubscriber<T> tees; // gives us access to an infinite stream of T values

    public T lastT = null;
    private Optional<BlackboxSubscriberProxy<T>> subProxy = Optional.empty();

    public BlackboxTestStage(TestEnvironment env) throws InterruptedException {
      this(env, true);
    }

    public BlackboxTestStage(TestEnvironment env, boolean runDefaultInit) throws InterruptedException {
      super(env);
      if (runDefaultInit) {
        pub = this.createHelperPublisher(Long.MAX_VALUE);
        tees = env.newManualSubscriber(pub);
        Subscriber<T> sub = createSubscriber();
        subProxy = Optional.of(createBlackboxSubscriberProxy(env, sub));
        subscribe(subProxy.get());
      }
    }

    public Subscriber<T> sub() {
      return subscriber.value();
    }

    /**
     * Proxy for the {@link #sub()} {@code Subscriber}, providing certain assertions on methods being called on the Subscriber.
     */
    public BlackboxSubscriberProxy<T> subProxy() {
      return subProxy.get();
    }

    public Publisher<T> createHelperPublisher(long elements) {
      return SubscriberBlackboxVerification.this.createHelperPublisher(elements);
    }

    public BlackboxSubscriberProxy<T> createBlackboxSubscriberProxy(TestEnvironment env, Subscriber<T> sub) {
      return new BlackboxSubscriberProxy<T>(env, sub);
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

  }

  public void notVerified() {
    throw new SkipException("Not verified using this TCK.");
  }
}