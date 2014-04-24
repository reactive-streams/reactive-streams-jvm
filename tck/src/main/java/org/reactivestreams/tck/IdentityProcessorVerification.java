package org.reactivestreams.tck;

import java.util.HashSet;
import java.util.Set;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment.ManualPublisher;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriberWithSubscriptionSupport;
import org.reactivestreams.tck.TestEnvironment.Promise;
import org.testng.annotations.Test;

public abstract class IdentityProcessorVerification<T> {

  private final TestEnvironment env;

  ////////////////////// DELEGATED TO SPECS //////////////////////

  // for delegating tests
  private final SubscriberVerification<T> subscriberVerification;

  // for delegating tests
  private final PublisherVerification<T> publisherVerification;

  ////////////////// END OF DELEGATED TO SPECS //////////////////


  private final int testBufferSize;

  /**
   * Test class must specify the expected time it takes for the publisher to
   * shut itself down when the the last downstream Subscription is cancelled.
   * Used by `publisherSubscribeWhenInShutDownStateMustTriggerOnErrorAndNotOnSubscribe`.
   */
  public IdentityProcessorVerification(TestEnvironment env, long publisherShutdownTimeoutMillis) {
    this(env, publisherShutdownTimeoutMillis, TestEnvironment.TEST_BUFFER_SIZE);
  }

  public IdentityProcessorVerification(final TestEnvironment env, long publisherShutdownTimeoutMillis, int testBufferSize) {
    this.env = env;
    this.testBufferSize = testBufferSize;

    this.subscriberVerification = new SubscriberVerification<T>(env) {
      @Override
      public Subscriber<T> createSubscriber(SubscriberProbe<T> probe) {
        return IdentityProcessorVerification.this.createSubscriber(probe);
      }

      @Override
      public Publisher<T> createHelperPublisher(int elements) {
        return IdentityProcessorVerification.this.createHelperPublisher(elements);
      }
    };

    publisherVerification = new PublisherVerification<T>(env, publisherShutdownTimeoutMillis) {
      @Override
      public Publisher<T> createPublisher(int elements) {
        return IdentityProcessorVerification.this.createPublisher(elements);
      }

      @Override
      public Publisher<T> createCompletedStatePublisher() {
        return IdentityProcessorVerification.this.createCompletedStatePublisher();
      }

      @Override
      public Publisher<T> createErrorStatePublisher() {
        return IdentityProcessorVerification.this.createErrorStatePublisher();
      }

    };
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a ReactiveSubject, which simply forwards all stream elements from its upstream
   * to its downstream. It must be able to internally buffer the given number of elements.
   */
  public abstract ReactiveSubject<T, T> createIdentityReactiveSubject(int bufferSize);

  /**
   * Helper method required for running the Publisher rules against a ReactiveSubject.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If `elements` is zero the produced stream must be infinite.
   * The stream must not produce the same element twice (in case of an infinite stream this requirement
   * is relaxed to only apply to the elements that are actually requested during all tests).
   */
  public abstract Publisher<T> createHelperPublisher(int elements);

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

  ////////////////////// PUBLISHER RULES VERIFICATION ///////////////////////////

  // A ReactiveSubject
  //   must obey all Publisher rules on its producing side
  public Publisher<T> createPublisher(int elements) {
    ReactiveSubject<T, T> processor = createIdentityReactiveSubject(testBufferSize);
    Publisher<T> pub = createHelperPublisher(elements);
    pub.subscribe(processor);
    return processor; // we run the PublisherVerification against this
  }

  // A Publisher
  //   must support a pending element count up to 2^63-1 (Long.MAX_VALUE) and provide for overflow protection
  @Test
  public void mustSupportAPendingElementCountUpToLongMaxValue() throws Exception {
    new TestSetup(env, testBufferSize) {{
      TestEnvironment.ManualSubscriber<T> sub = newSubscriber();
      sub.request(Integer.MAX_VALUE);
      sub.request(Integer.MAX_VALUE);
      sub.request(2); // if the Subscription only keeps an int counter without overflow protection it will now be at zero

      final T x = sendNextTFromUpstream();
      expectNextElement(sub, x);

      final T y = sendNextTFromUpstream();
      expectNextElement(sub, y);

      // to avoid error messages during test harness shutdown
      sendCompletion();
      sub.expectCompletion(env.defaultTimeoutMillis());

      env.verifyNoAsyncErrors();
    }};
  }

  @Test
  public void createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
    publisherVerification.createPublisher3MustProduceAStreamOfExactly3Elements();
  }

  @Test
  public void mustCallOnCompleteOnASubscriberAfterHavingProducedTheFinalStreamElementToIt() throws Throwable {
    publisherVerification.mustCallOnCompleteOnASubscriberAfterHavingProducedTheFinalStreamElementToIt();
  }

  @Test
  public void mustStartProducingWithTheOldestStillAvailableElementForASubscriber() {
    publisherVerification.mustStartProducingWithTheOldestStillAvailableElementForASubscriber();
  }

  // A Publisher
  //   must call `onError` on all its subscribers if it encounters a non-recoverable error
  @Test
  public void mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError() throws Exception {
    new TestSetup(env, testBufferSize) {{
      ManualSubscriberWithErrorCollection<T> sub1 = new ManualSubscriberWithErrorCollection<T>(env);
      env.subscribe(processor, sub1);
      ManualSubscriberWithErrorCollection<T> sub2 = new ManualSubscriberWithErrorCollection<T>(env);
      env.subscribe(processor, sub2);

      sub1.request(1);
      expectRequestMore();
      final T x = sendNextTFromUpstream();
      expectNextElement(sub1, x);
      sub1.request(1);

      // sub1 now has received and element and has 1 pending
      // sub2 has not yet requested anything

      Exception ex = new RuntimeException("Test exception");
      sendError(ex);
      sub1.expectError(ex);
      sub2.expectError(ex);

      env.verifyNoAsyncErrors();
    }};
  }

  @Test
  public void mustNotCallOnCompleteOrOnErrorMoreThanOncePerSubscriber() {
    publisherVerification.mustNotCallOnCompleteOrOnErrorMoreThanOncePerSubscriber();
  }

  ////////////////////// SUBSCRIBER RULES VERIFICATION ///////////////////////////

  // A ReactiveSubject
  //   must obey all Subscriber rules on its consuming side
  public Subscriber<T> createSubscriber(final SubscriberVerification.SubscriberProbe<T> probe) {
    ReactiveSubject<T, T> processor = createIdentityReactiveSubject(testBufferSize);
    processor.subscribe(
        new Subscriber<T>() {
          public void onSubscribe(final Subscription subscription) {
            probe.registerOnSubscribe(
                new SubscriberVerification.SubscriberPuppet() {
                  public void triggerShutdown() {
                    subscription.cancel();
                  }

                  public void triggerRequestMore(int elements) {
                    subscription.request(elements);
                  }

                  public void triggerCancel() {
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

  ////////////////////// OTHER SPEC RULE VERIFICATION ///////////////////////////

  // A ReactiveSubject
  //   must cancel its upstream Subscription if its last downstream Subscription has been cancelled
  @Test
  public void mustCancelItsUpstreamSubscriptionIfItsLastDownstreamSubscriptionHasBeenCancelled() throws Exception {
    new TestSetup(env, testBufferSize) {{
      TestEnvironment.ManualSubscriber<T> sub = newSubscriber();
      sub.cancel();
      expectCancelling();

      env.verifyNoAsyncErrors();
    }};
  }

  // A ReactiveSubject
  //   must immediately pass on `onError` events received from its upstream to its downstream
  @Test
  public void mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream() throws Exception {
    new TestSetup(env, testBufferSize) {{
      ManualSubscriberWithErrorCollection<T> sub = new ManualSubscriberWithErrorCollection<T>(env);
      env.subscribe(processor, sub);

      Exception ex = new RuntimeException("Test exception");
      sendError(ex);
      sub.expectError(ex); // "immediately", i.e. without a preceding request

      env.verifyNoAsyncErrors();
    }};
  }

  // A ReactiveSubject
  //   must be prepared to receive incoming elements from its upstream even if a downstream subscriber has not requested anything yet
  @Test
  public void mustBePreparedToReceiveIncomingElementsFromItsUpstreamEvenIfADownstreamSubscriberHasNotRequestedYet() throws Exception {
    new TestSetup(env, testBufferSize) {{
        ManualSubscriber<T> sub = newSubscriber();
        final T x = sendNextTFromUpstream();
        sub.expectNone(50);
        final T y = sendNextTFromUpstream();
        sub.expectNone(50);

        sub.request(2);
        sub.expectNext(x);
        sub.expectNext(y);

        // to avoid error messages during test harness shutdown
        sendCompletion();
        sub.expectCompletion(env.defaultTimeoutMillis());

        env.verifyNoAsyncErrors();
      }};
  }

  /////////////////////// DELEGATED TESTS, A PROCESSOR "IS A" SUBSCRIBER //////////////////////

  @Test
  public void exerciseHappyPath() throws InterruptedException {
    subscriberVerification.exerciseHappyPath();
  }

  @Test
  public void onSubscribeAndOnNextMustAsynchronouslyScheduleAnEvent() {
    subscriberVerification.onSubscribeAndOnNextMustAsynchronouslyScheduleAnEvent();
  }

  @Test
  public void onCompleteAndOnErrorMustAsynchronouslyScheduleAnEvent() {
    subscriberVerification.onCompleteAndOnErrorMustAsynchronouslyScheduleAnEvent();
  }

  @Test
  public void mustNotAcceptAnOnSubscribeEventIfItAlreadyHasAnActiveSubscription() throws InterruptedException {
    subscriberVerification.mustNotAcceptAnOnSubscribeEventIfItAlreadyHasAnActiveSubscription();
  }

  @Test
  public void mustCallSubscriptionCancelDuringShutdownIfItStillHasAnActiveSubscription() throws InterruptedException {
    subscriberVerification.mustCallSubscriptionCancelDuringShutdownIfItStillHasAnActiveSubscription();
  }

  @Test
  public void mustEnsureThatAllCallsOnASubscriptionTakePlaceFromTheSameThreadOrProvideExternalSync() {
    subscriberVerification.mustEnsureThatAllCallsOnASubscriptionTakePlaceFromTheSameThreadOrProvideExternalSync();
  }

  @Test
  public void mustBePreparedToReceiveOneOrMoreOnNextEventsAfterHavingCalledSubscriptionCancel() throws InterruptedException {
    subscriberVerification.mustBePreparedToReceiveOneOrMoreOnNextEventsAfterHavingCalledSubscriptionCancel();
  }

  @Test
  public void mustBePreparedToReceiveAnOnCompleteEventWithAPrecedingSubscriptionRequestMore() throws InterruptedException {
    subscriberVerification.mustBePreparedToReceiveAnOnCompleteEventWithAPrecedingSubscriptionRequestMore();
  }

  @Test
  public void mustBePreparedToReceiveAnOnCompleteEventWithoutAPrecedingSubscriptionRequestMore() throws InterruptedException {
    subscriberVerification.mustBePreparedToReceiveAnOnCompleteEventWithoutAPrecedingSubscriptionRequestMore();
  }

  @Test
  public void mustBePreparedToReceiveAnOnErrorEventWithAPrecedingSubscriptionRequestMore() throws InterruptedException {
    subscriberVerification.mustBePreparedToReceiveAnOnErrorEventWithAPrecedingSubscriptionRequestMore();
  }

  @Test
  public void mustBePreparedToReceiveAnOnErrorEventWithoutAPrecedingSubscriptionRequestMore() throws InterruptedException {
    subscriberVerification.mustBePreparedToReceiveAnOnErrorEventWithoutAPrecedingSubscriptionRequestMore();
  }

  @Test
  public void mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() {
    subscriberVerification.mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents();
  }


  /////////////////////// DELEGATED TESTS, A PROCESSOR "IS A" PUBLISHER //////////////////////

  @Test
  public void publisherSubscribeWhenCompletedMustTriggerOnCompleteAndNotOnSubscribe() throws Throwable {
    publisherVerification.publisherSubscribeWhenCompletedMustTriggerOnCompleteAndNotOnSubscribe();
  }

  @Test
  public void publisherSubscribeWhenInErrorStateMustTriggerOnErrorAndNotOnSubscribe() throws Throwable {
    publisherVerification.publisherSubscribeWhenInErrorStateMustTriggerOnErrorAndNotOnSubscribe();
  }

  @Test
  public void publisherSubscribeWhenInShutDownStateMustTriggerOnErrorAndNotOnSubscribe() throws Throwable {
    publisherVerification.publisherSubscribeWhenInShutDownStateMustTriggerOnErrorAndNotOnSubscribe();
  }

  @Test
  public void publisherSubscribeWhenActiveMustCallOnSubscribeFirst() throws Throwable {
    publisherVerification.publisherSubscribeWhenActiveMustCallOnSubscribeFirst();
  }

  @Test
  public void publisherSubscribeWhenActiveMustRejectDoubleSubscription() throws Throwable {
    publisherVerification.publisherSubscribeWhenActiveMustRejectDoubleSubscription();
  }

  @Test
  public void subscriptionRequestMoreWhenCancelledMustIgnoreTheCall() throws Throwable {
    publisherVerification.subscriptionRequestMoreWhenCancelledMustIgnoreTheCall();
  }

  @Test
  public void subscriptionRequestMoreMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
    publisherVerification.subscriptionRequestMoreMustResultInTheCorrectNumberOfProducedElements();
  }

  @Test
  public void subscriptionRequestMoreMustThrowIfArgumentIsNonPositive() throws Throwable {
    publisherVerification.subscriptionRequestMoreMustThrowIfArgumentIsNonPositive();
  }

  @Test
  public void subscriptionCancelWhenCancelledMustIgnoreCall() throws Throwable {
    publisherVerification.subscriptionCancelWhenCancelledMustIgnoreCall();
  }

  @Test
  public void onSubscriptionCancelThePublisherMustEventuallyCeaseToCallAnyMethodsOnTheSubscriber() throws Throwable {
    publisherVerification.onSubscriptionCancelThePublisherMustEventuallyCeaseToCallAnyMethodsOnTheSubscriber();
  }

  @Test
  public void onSubscriptionCancelThePublisherMustEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
    publisherVerification.onSubscriptionCancelThePublisherMustEventuallyDropAllReferencesToTheSubscriber();
  }

  @Test
  public void mustNotCallOnNextAfterHavingIssuedAnOnCompleteOrOnErrorCallOnASubscriber() {
    publisherVerification.mustNotCallOnNextAfterHavingIssuedAnOnCompleteOrOnErrorCallOnASubscriber();
  }

  @Test
  public void mustProduceTheSameElementsInTheSameSequenceForAllItsSubscribers() throws Throwable {
    publisherVerification.mustProduceTheSameElementsInTheSameSequenceForAllItsSubscribers();
  }


  /////////////////////// ADDITIONAL "COROLLARY" TESTS //////////////////////

  @Test // trigger `requestFromUpstream` for elements that have been requested 'long ago'
  public void mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo() throws Exception {
    new TestSetup(env, testBufferSize) {{
      TestEnvironment.ManualSubscriber<T> sub1 = newSubscriber();
      sub1.request(20);

      int totalRequests = expectRequestMore();
      final T x = sendNextTFromUpstream();
      expectNextElement(sub1, x);

      if (totalRequests == 1) {
        totalRequests += expectRequestMore();
      }
      final T y = sendNextTFromUpstream();
      expectNextElement(sub1, y);

      if (totalRequests == 2) {
        totalRequests += expectRequestMore();
      }

      TestEnvironment.ManualSubscriber<T> sub2 = newSubscriber();

      // sub1 now has 18 pending
      // sub2 has 0 pending

      final T z = sendNextTFromUpstream();
      expectNextElement(sub1, z);
      sub2.expectNone(); // since sub2 hasn't requested anything yet

      sub2.request(1);
      expectNextElement(sub2, z);

      if (totalRequests == 3) {
        expectRequestMore();
      }

      // to avoid error messages during test harness shutdown
      sendCompletion();
      sub1.expectCompletion(env.defaultTimeoutMillis());
      sub2.expectCompletion(env.defaultTimeoutMillis());

      env.verifyNoAsyncErrors();
    }};
  }

  @Test // unblock the stream if a 'blocking' subscription has been cancelled
  @SuppressWarnings("unchecked")
  public void mustUnblockTheStreamIfABlockingSubscriptionHasBeenCancelled() throws InterruptedException {
    new TestSetup(env, testBufferSize) {{
      TestEnvironment.ManualSubscriber<T> sub1 = newSubscriber();
      TestEnvironment.ManualSubscriber<T> sub2 = newSubscriber();

      sub1.request(testBufferSize + 1);
      int pending = 0;
      int sent = 0;
      final T[] tees = (T[]) new Object[testBufferSize];
      while (sent < testBufferSize) {
        if (pending == 0) {
          pending = expectRequestMore();
        }
        tees[sent] = nextT();
        sendNext(tees[sent]);
        sent += 1;
        pending -= 1;
      }

      expectNoRequestMore(); // because we only have buffer size testBufferSize and sub2 hasn't seen the first value yet
      sub2.cancel(); // must "unblock"

      expectRequestMore();
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
    private TestEnvironment.ManualSubscriber<T> tees; // gives us access to an infinite stream of T values
    private Set<T> seenTees = new HashSet<T>();

    final ReactiveSubject<T, T> processor;
    final int testBufferSize;

    public TestSetup(TestEnvironment env, int testBufferSize) throws InterruptedException {
      super(env);
      this.testBufferSize = testBufferSize;
      tees = env.newManualSubscriber(createHelperPublisher(0));
      processor = createIdentityReactiveSubject(testBufferSize);
      subscribe(processor);
    }

    public TestEnvironment.ManualSubscriber<T> newSubscriber() throws InterruptedException {
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

    public void expectNextElement(TestEnvironment.ManualSubscriber<T> sub, T expected) throws InterruptedException {
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
    TestEnvironment.Promise<Throwable> error;

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