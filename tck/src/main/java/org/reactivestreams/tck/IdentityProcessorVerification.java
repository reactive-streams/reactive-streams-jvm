package org.reactivestreams.tck;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.ManualPublisher;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriberWithSubscriptionSupport;
import org.reactivestreams.tck.TestEnvironment.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public abstract class IdentityProcessorVerification<T> {

  private final TestEnvironment env;

  ////////////////////// DELEGATED TO SPECS //////////////////////

  // for delegating tests
  private final SubscriberWhiteboxVerification<T> subscriberVerification;

  // for delegating tests
  private final PublisherVerification<T> publisherVerification;

  ////////////////// END OF DELEGATED TO SPECS //////////////////

  // number of elements the processor under test must be able ot buffer,
  // without dropping elements. Defaults to `TestEnvironment.TEST_BUFFER_SIZE`.
  private final int processorBufferSize;

  /**
   * Test class must specify the expected time it takes for the publisher to
   * shut itself down when the the last downstream {@code Subscription} is cancelled.
   *
   * The processor will be required to be able to buffer {@code TestEnvironment.TEST_BUFFER_SIZE} elements.
   *
   * @param publisherShutdownTimeoutMillis expected time which a processor requires to shut itself down
   */
  @SuppressWarnings("unused")
  public IdentityProcessorVerification(final TestEnvironment env, long publisherShutdownTimeoutMillis) {
    this(env, publisherShutdownTimeoutMillis, TestEnvironment.TEST_BUFFER_SIZE);
  }

  /**
   * Test class must specify the expected time it takes for the publisher to
   * shut itself down when the the last downstream {@code Subscription} is cancelled.
   *
   * @param publisherShutdownTimeoutMillis expected time which a processor requires to shut itself down
   * @param processorBufferSize            number of elements the processor is required to be able to buffer.
   */
  public IdentityProcessorVerification(final TestEnvironment env, long publisherShutdownTimeoutMillis, int processorBufferSize) {
    this.env = env;
    this.processorBufferSize = processorBufferSize;

    this.subscriberVerification = new SubscriberWhiteboxVerification<T>(env) {
      @Override
      public Subscriber<T> createSubscriber(WhiteboxSubscriberProbe<T> probe) {
        return IdentityProcessorVerification.this.createSubscriber(probe);
      }

      @Override
      public Publisher<T> createHelperPublisher(long elements) {
        return IdentityProcessorVerification.this.createHelperPublisher(elements);
      }
    };

    publisherVerification = new PublisherVerification<T>(env, publisherShutdownTimeoutMillis) {
      @Override
      public Publisher<T> createPublisher(long elements) {
        return IdentityProcessorVerification.this.createPublisher(elements);
      }

      @Override
      public Publisher<T> createErrorStatePublisher() {
        return IdentityProcessorVerification.this.createErrorStatePublisher();
      }

      @Override
      public long maxElementsFromPublisher() {
        return IdentityProcessorVerification.this.maxElementsFromPublisher();
      }

      @Override
      public long boundedDepthOfOnNextAndRequestRecursion() {
        return IdentityProcessorVerification.this.boundedDepthOfOnNextAndRequestRecursion();
      }
    };
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a Publisher, which simply forwards all stream elements from its upstream
   * to its downstream. It must be able to internally buffer the given number of elements.
   *
   * @param bufferSize number of elements the processor is required to be able to buffer.
   */
  public abstract Processor<T, T> createIdentityProcessor(int bufferSize);

  /**
   * Helper method required for running the Publisher rules against a Publisher.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If {@code elements} is {@code Long.MAX_VALUE} the produced stream must be infinite.
   *
   * The stream must not produce the same element twice (in case of an infinite stream this requirement
   * is relaxed to only apply to the elements that are actually requested during all tests).
   *
   * @param elements exact number of elements this publisher should emit,
   *                 unless equal to {@code Long.MAX_VALUE} in which case the stream should be effectively infinite
   */
  public abstract Publisher<T> createHelperPublisher(long elements);

  /**
   * Return a Publisher that immediately signals {@code onError} to incoming subscriptions,
   * or {@code null} in order to skip them.
   */
  public abstract Publisher<T> createErrorStatePublisher();

  /**
   * Override and return lower value if your Publisher is only able to produce a finite number of elements.
   * For example, if it is designed to return at-most-one element, return {@code 1} from this method.
   *
   * Defaults to {@code Long.MAX_VALUE}, meaning that the Publisher can be produce up to infinite streams.
   */
  public long maxElementsFromPublisher() {
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
    publisherVerification.setUp();
    subscriberVerification.setUp();
  }

  ////////////////////// PUBLISHER RULES VERIFICATION ///////////////////////////

  // A Processor
  //   must obey all Publisher rules on its publishing side
  public Publisher<T> createPublisher(long elements) {
    final Processor<T, T> processor = createIdentityProcessor(processorBufferSize);
    final Publisher<T> pub = createHelperPublisher(elements);
    pub.subscribe(processor);
    return processor; // we run the PublisherVerification against this
  }

  @Test
  public void validate_maxElementsFromPublisher() throws Exception {
    publisherVerification.validate_maxElementsFromPublisher();
  }

  @Test
  public void validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
    publisherVerification.validate_boundedDepthOfOnNextAndRequestRecursion();
  }

  /////////////////////// DELEGATED TESTS, A PROCESSOR "IS A" PUBLISHER //////////////////////
  // Verifies rule: https://github.com/reactive-streams/reactive-streams#4.1

  @Test
  public void createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
    publisherVerification.createPublisher1MustProduceAStreamOfExactly1Element();
  }

  @Test
  public void createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
    publisherVerification.createPublisher3MustProduceAStreamOfExactly3Elements();
  }

  @Test
  public void spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
    publisherVerification.spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements();
  }

  @Test
  public void spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
    publisherVerification.spec102_maySignalLessThanRequestedAndTerminateSubscription();
  }

  @Test
  public void spec103_mustSignalOnMethodsSequentially() throws Throwable {
    publisherVerification.spec103_mustSignalOnMethodsSequentially();
  }

  @Test
  public void spec104_mustSignalOnErrorWhenFails() throws Throwable {
    publisherVerification.spec104_mustSignalOnErrorWhenFails();
  }

  @Test
  public void spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
    publisherVerification.spec105_mustSignalOnCompleteWhenFiniteStreamTerminates();
  }

  @Test
  public void spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable {
    publisherVerification.spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled();
  }

  @Test
  public void spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable {
    publisherVerification.spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
  }

  @Test
  public void spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable {
    publisherVerification.spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled();
  }

  @Test
  public void spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable {
    publisherVerification.spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals();
  }

  @Test
  public void spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable {
    publisherVerification.spec109_subscribeShouldNotThrowNonFatalThrowable();
  }

  @Test
  public void spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable {
    publisherVerification.spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice();
  }

  @Test
  public void spec111_maySupportMultiSubscribe() throws Throwable {
    publisherVerification.spec111_maySupportMultiSubscribe();
  }

  @Test
  public void spec112_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe() throws Throwable {
    publisherVerification.spec112_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe();
  }

  @Test
  public void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
    publisherVerification.spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
  }

  @Test
  public void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
    publisherVerification.spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
  }

  @Test
  public void spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
    publisherVerification.spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe();
  }

  @Test
  public void spec303_mustNotAllowUnboundedRecursion() throws Throwable {
    publisherVerification.spec303_mustNotAllowUnboundedRecursion();
  }

  @Test
  public void spec304_requestShouldNotPerformHeavyComputations() throws Exception {
    publisherVerification.spec304_requestShouldNotPerformHeavyComputations();
  }

  @Test
  public void spec305_cancelMustNotSynchronouslyPerformHeavyCompuatation() throws Exception {
    publisherVerification.spec305_cancelMustNotSynchronouslyPerformHeavyCompuatation();
  }

  @Test
  public void spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
    publisherVerification.spec306_afterSubscriptionIsCancelledRequestMustBeNops();
  }

  @Test
  public void spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable {
    publisherVerification.spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
  }

  @Test
  public void spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
    publisherVerification.spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
  }

  @Test
  public void spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable {
    publisherVerification.spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams#1.4
  // for multiple subscribers
  @Test
  public void spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError() throws Exception {
    new TestSetup(env, processorBufferSize) {{
      final ManualSubscriberWithErrorCollection<T> sub1 = new ManualSubscriberWithErrorCollection<T>(env);
      env.subscribe(processor, sub1);

      final ManualSubscriberWithErrorCollection<T> sub2 = new ManualSubscriberWithErrorCollection<T>(env);
      env.subscribe(processor, sub2);

      sub1.request(1);
      expectRequest();
      final T x = sendNextTFromUpstream();
      expectNextElement(sub1, x);
      sub1.request(1);

      // sub1 has received one element, and has one demand pending
      // sub2 has not yet requested anything

      final Exception ex = new RuntimeException("Test exception");
      sendError(ex);
      sub1.expectError(ex);
      sub2.expectError(ex);

      env.verifyNoAsyncErrors();
    }};
  }

  ////////////////////// SUBSCRIBER RULES VERIFICATION ///////////////////////////
  // Verifies rule: https://github.com/reactive-streams/reactive-streams#4.1

  // A Processor
  //   must obey all Subscriber rules on its consuming side
  public Subscriber<T> createSubscriber(final SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<T> probe) {
    final Processor<T, T> processor = createIdentityProcessor(processorBufferSize);
    processor.subscribe(
        new Subscriber<T>() {
          public void onSubscribe(final Subscription subscription) {
            probe.registerOnSubscribe(new SubscriberWhiteboxVerification.SubscriberPuppet() {
              public void triggerShutdown() {
                subscription.cancel();
              }

              public void triggerRequest(long elements) {
                subscription.request(elements);
              }

              public void signalCancel() {
                subscription.cancel();
              }
            });
          }

          public void onNext(T element) {
            probe.registerOnNext(element);
          }

          public void onComplete() {
            probe.registerOnComplete();
          }

          public void onError(Throwable cause) {
            probe.registerOnError(cause);
          }
        });

    return processor; // we run the SubscriberVerification against this
  }

  ////////////////////// OTHER RULE VERIFICATION ///////////////////////////

  // A Processor
  //   must cancel its upstream Subscription if its last downstream Subscription has been cancelled
  @Test
  public void mustCancelItsUpstreamSubscriptionIfItsLastDownstreamSubscriptionHasBeenCancelled() throws Exception {
    new TestSetup(env, processorBufferSize) {{
      final ManualSubscriber<T> sub = newSubscriber();
      sub.cancel();
      expectCancelling();

      env.verifyNoAsyncErrors();
    }};
  }

  // A Processor
  //   must immediately pass on `onError` events received from its upstream to its downstream
  @Test
  public void mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream() throws Exception {
    new TestSetup(env, processorBufferSize) {{
      final ManualSubscriberWithErrorCollection<T> sub = new ManualSubscriberWithErrorCollection<T>(env);
      env.subscribe(processor, sub);

      final Exception ex = new RuntimeException("Test exception");
      sendError(ex);
      sub.expectError(ex); // "immediately", i.e. without a preceding request

      env.verifyNoAsyncErrors();
    }};
  }

  /////////////////////// DELEGATED TESTS, A PROCESSOR "IS A" SUBSCRIBER //////////////////////
  // Verifies rule: https://github.com/reactive-streams/reactive-streams#4.1

  @Test
  public void exerciseWhiteboxHappyPath() throws Throwable {
    subscriberVerification.exerciseWhiteboxHappyPath();
  }

  @Test
  public void spec317_mustSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
    subscriberVerification.spec317_mustSignalOnErrorWhenPendingAboveLongMaxValue();
  }

  @Test
  public void spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable {
    subscriberVerification.spec201_mustSignalDemandViaSubscriptionRequest();
  }

  @Test
  public void spec202_shouldAsynchronouslyDispatch() throws Exception {
    subscriberVerification.spec202_shouldAsynchronouslyDispatch();
  }

  @Test
  public void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable {
    subscriberVerification.spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
  }

  @Test
  public void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable {
    subscriberVerification.spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
  }

  @Test
  public void spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception {
    subscriberVerification.spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError();
  }

  @Test
  public void spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception {
    subscriberVerification.spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
  }

  @Test
  public void spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception {
    subscriberVerification.spec206_mustCallSubscriptionCancelIfItIsNoLongerValid();
  }

  @Test
  public void spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThread() throws Exception {
    subscriberVerification.spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThread();
  }

  @Test
  public void spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable {
    subscriberVerification.spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel();
  }

  @Test
  public void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
    subscriberVerification.spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
  }

  @Test
  public void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
    subscriberVerification.spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
  }

  @Test
  public void spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
    subscriberVerification.spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
  }

  @Test
  public void spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable {
    subscriberVerification.spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall();
  }

  @Test
  public void spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception {
    subscriberVerification.spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents();
  }

  @Test
  public void spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable {
    subscriberVerification.spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality();
  }

  @Test
  public void spec213_failingOnCompleteInvocation() throws Exception {
    subscriberVerification.spec213_failingOnCompleteInvocation();
  }

  @Test
  public void spec214_failingOnErrorInvocation() throws Exception {
    subscriberVerification.spec214_failingOnErrorInvocation();
  }

  @Test
  public void spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception {
    subscriberVerification.spec301_mustNotBeCalledOutsideSubscriberContext();
  }

  @Test
  public void spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
    subscriberVerification.spec308_requestMustRegisterGivenNumberElementsToBeProduced();
  }

  @Test
  public void spec309_callingRequestZeroMustThrow() throws Throwable {
    subscriberVerification.spec309_callingRequestZeroMustThrow();
  }

  @Test
  public void spec309_callingRequestWithNegativeNumberMustThrow() throws Throwable {
    subscriberVerification.spec309_callingRequestWithNegativeNumberMustThrow();
  }

  @Test
  public void spec310_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception {
    subscriberVerification.spec310_requestMaySynchronouslyCallOnNextOnSubscriber();
  }

  @Test
  public void spec311_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception {
    subscriberVerification.spec311_requestMaySynchronouslyCallOnCompleteOrOnError();
  }

  @Test
  public void spec312_cancelMustRequestThePublisherToEventuallyStopSignaling() throws Throwable {
    subscriberVerification.spec312_cancelMustRequestThePublisherToEventuallyStopSignaling();
  }

  @Test
  public void spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception {
    subscriberVerification.spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists();
  }

  @Test
  public void spec315_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception {
    subscriberVerification.spec315_cancelMustNotThrowExceptionAndMustSignalOnError();
  }

  @Test
  public void spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception {
    subscriberVerification.spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber();
  }

  @Test
  public void spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
    subscriberVerification.spec317_mustSupportAPendingElementCountUpToLongMaxValue();
  }

  /////////////////////// ADDITIONAL "COROLLARY" TESTS //////////////////////

  // A Processor
  //   must trigger `requestFromUpstream` for elements that have been requested 'long ago'
  @Test
  public void mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo() throws Exception {
    new TestSetup(env, processorBufferSize) {{
      ManualSubscriber<T> sub1 = newSubscriber();
      sub1.request(20);

      long totalRequests = expectRequest();
      final T x = sendNextTFromUpstream();
      expectNextElement(sub1, x);

      if (totalRequests == 1) {
        totalRequests += expectRequest();
      }
      final T y = sendNextTFromUpstream();
      expectNextElement(sub1, y);

      if (totalRequests == 2) {
        totalRequests += expectRequest();
      }

      ManualSubscriber<T> sub2 = newSubscriber();

      // sub1 now has 18 pending
      // sub2 has 0 pending

      final T z = sendNextTFromUpstream();
      expectNextElement(sub1, z);
      sub2.expectNone(); // since sub2 hasn't requested anything yet

      sub2.request(1);
      expectNextElement(sub2, z);

      if (totalRequests == 3) {
        expectRequest();
      }

      // to avoid error messages during test harness shutdown
      sendCompletion();
      sub1.expectCompletion(env.defaultTimeoutMillis());
      sub2.expectCompletion(env.defaultTimeoutMillis());

      env.verifyNoAsyncErrors();
    }};
  }

  // A Processor
  //   must unblock the stream if a 'blocking' subscription has been cancelled
  @Test
  @SuppressWarnings("unchecked")
  public void mustUnblockTheStreamIfABlockingSubscriptionHasBeenCancelled() throws InterruptedException {
    new TestSetup(env, processorBufferSize) {{
      ManualSubscriber<T> sub1 = newSubscriber();
      ManualSubscriber<T> sub2 = newSubscriber();

      sub1.request(processorBufferSize + 1);
      long pending = 0;
      int sent = 0;
      final T[] tees = (T[]) new Object[processorBufferSize];
      while (sent < processorBufferSize) {
        if (pending == 0) {
          pending = expectRequest();
        }
        tees[sent] = nextT();
        sendNext(tees[sent]);
        sent += 1;
        pending -= 1;
      }

      expectNoRequest(); // because we only have buffer size processorBufferSize and sub2 hasn't seen the first value yet
      sub2.cancel(); // must "unblock"

      expectRequest();
      for (T tee : tees) {
        expectNextElement(sub1, tee);
      }

      sendCompletion();
      sub1.expectCompletion(env.defaultTimeoutMillis());

      env.verifyNoAsyncErrors();
    }};
  }

  /////////////////////// TEST INFRASTRUCTURE //////////////////////

  public abstract class TestSetup extends ManualPublisher<T> {
    final private ManualSubscriber<T> tees; // gives us access to an infinite stream of T values
    private Set<T> seenTees = new HashSet<T>();

    final Processor<T, T> processor;

    public TestSetup(TestEnvironment env, int testBufferSize) throws InterruptedException {
      super(env);
      tees = env.newManualSubscriber(createHelperPublisher(Long.MAX_VALUE));
      processor = createIdentityProcessor(testBufferSize);
      subscribe(processor);
    }

    public ManualSubscriber<T> newSubscriber() throws InterruptedException {
      return env.newManualSubscriber(processor);
    }

    public T nextT() throws InterruptedException {
      final T t = tees.requestNextElement();
      if (seenTees.contains(t)) {
        env.flop("Helper publisher illegally produced the same element " + t + " twice");
      }
      seenTees.add(t);
      return t;
    }

    public void expectNextElement(ManualSubscriber<T> sub, T expected) throws InterruptedException {
      final T elem = sub.nextElement("timeout while awaiting " + expected);
      if (!elem.equals(expected)) {
        env.flop("Received `onNext(" + elem + ")` on downstream but expected `onNext(" + expected + ")`");
      }
    }

    public T sendNextTFromUpstream() throws InterruptedException {
      final T x = nextT();
      sendNext(x);
      return x;
    }
  }

  public class ManualSubscriberWithErrorCollection<A> extends ManualSubscriberWithSubscriptionSupport<A> {
    Promise<Throwable> error;

    public ManualSubscriberWithErrorCollection(TestEnvironment env) {
      super(env);
      error = new Promise<Throwable>(env);
    }

    @Override
    public void onError(Throwable cause) {
      error.complete(cause);
    }

    public void expectError(Throwable cause) throws InterruptedException {
      expectError(cause, env.defaultTimeoutMillis());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void expectError(Throwable cause, long timeoutMillis) throws InterruptedException {
      error.expectCompletion(timeoutMillis, "Did not receive expected error on downstream");
      if (!error.value().equals(cause)) {
        env.flop("Expected error " + cause + " but got " + error.value());
      }
    }
  }
}