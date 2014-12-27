package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.support.TCKVerificationSupport;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
* Validates that the TCK's {@link org.reactivestreams.tck.SubscriberBlackboxVerification} fails with nice human readable errors.
* <b>Important: Please note that all Subscribers implemented in this file are *wrong*!</b>
*/
public class SubscriberBlackboxVerificationTest extends TCKVerificationSupport {

  static final int DEFAULT_TIMEOUT_MILLIS = 100;

  @Test
  public void spec201_blackbox_mustSignalDemandViaSubscriptionRequest_shouldFailBy_notGettingRequestCall() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopSubscriberVerification().spec201_blackbox_mustSignalDemandViaSubscriptionRequest();
      }
    }, "Did not receive expected `request` call within");
  }

  @Test
  public void spec201_blackbox_mustSignalDemandViaSubscriptionRequest_shouldPass() throws Throwable {
    simpleSubscriberVerification().spec201_blackbox_mustSignalDemandViaSubscriptionRequest();
  }

  @Test
  public void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete_shouldFail_dueToCallingRequest() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onComplete() {
            subscription.request(1);
          }
        }).spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
      }
    }, "Subscription::request MUST NOT be called from Subscriber::onComplete (Rule 2.3)!");
  }

  @Test
  public void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete_shouldFail_dueToCallingCancel() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onComplete() {
            subscription.cancel();
          }
        }).spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
      }
    }, "Subscription::cancel MUST NOT be called from Subscriber::onComplete (Rule 2.3)!");
  }

  @Test
  public void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError_shouldFail_dueToCallingRequest() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onError(Throwable t) {
            subscription.request(1);
          }
        }).spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
      }
    }, "Subscription::request MUST NOT be called from Subscriber::onError (Rule 2.3)!");
  }
  @Test
  public void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError_shouldFail_dueToCallingCancel() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onError(Throwable t) {
            subscription.cancel();
          }
        }).spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
      }
    }, "Subscription::cancel MUST NOT be called from Subscriber::onError (Rule 2.3)!");
  }

  @Test
  public void spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onSubscribe(Subscription s) {
            super.onSubscribe(s);

            s.request(1); // this is wrong, as one should always check if should accept or reject the subscription
          }
        }).spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
      }
    }, "illegally called `subscription.request(1)");
  }

  @Test
  public void spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new NoopSubscriber() {
          // don't even request()
        }).spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
      }
    }, "did not call `registerOnComplete()`");
  }

  @Test
  public void spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall_shouldPass_withNoopSubscriber() throws Throwable {
    customSubscriberVerification(new NoopSubscriber() {
      // don't even request()
    }).spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
  }

  @Test
  public void spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {

        customSubscriberVerification(new NoopSubscriber() {
          @Override public void onError(Throwable t) {
            // this is wrong in many ways (incl. spec violation), but aims to simulate user code which "blows up" when handling the onError signal
            throw new RuntimeException("Wrong, don't do this!", t); // don't do this
          }
        }).spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
      }
    }, "Test Exception: Boom!"); // checks that the expected exception was delivered to onError, we don't expect anyone to implement onError so weirdly
  }

  // FAILING IMPLEMENTATIONS //

  /**
   * Verification using a Subscriber that doesn't do anything on any of the callbacks
   */
  final SubscriberBlackboxVerification<Integer> noopSubscriberVerification() {
    return new SubscriberBlackboxVerification<Integer>(newTestEnvironment()) {
      @Override public Subscriber<Integer> createSubscriber() {
        return new NoopSubscriber();
      }

      @Override public Publisher<Integer> createHelperPublisher(long elements) {
        return newSimpleIntsPublisher(elements);
      }
    };
  }

  /**
   * Verification using a Subscriber that doesn't do anything on any of the callbacks
   */
  final SubscriberBlackboxVerification<Integer> simpleSubscriberVerification() {
    return new SubscriberBlackboxVerification<Integer>(newTestEnvironment()) {
      @Override public Subscriber<Integer> createSubscriber() {
        return new NoopSubscriber() {
          volatile Subscription subscription;

          @Override public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
          }

          @Override public void onNext(Integer element) {
            subscription.request(1);
          }
        };
      }

      @Override public Publisher<Integer> createHelperPublisher(long elements) {
        return newSimpleIntsPublisher(elements);
      }
    };
  }

  /**
   * Custom Verification using given Subscriber
   */
  final SubscriberBlackboxVerification<Integer> customSubscriberVerification(final Subscriber<Integer> sub) {
    return new SubscriberBlackboxVerification<Integer>(newTestEnvironment()) {
      @Override public Subscriber<Integer> createSubscriber() {
        return sub;
      }

      @Override public Publisher<Integer> createHelperPublisher(long elements) {
        return newSimpleIntsPublisher(elements);
      }
    };
  }

  static class NoopSubscriber implements Subscriber<Integer> {

    @Override public void onSubscribe(Subscription s) {
      // noop
    }

    @Override public void onNext(Integer element) {
      // noop
    }

    @Override public void onError(Throwable t) {
      // noop
    }

    @Override public void onComplete() {
      // noop
    }
  }

  static class KeepSubscriptionSubscriber extends NoopSubscriber {
    volatile Subscription subscription;

    @Override public void onSubscribe(Subscription s) {
      this.subscription = s;
    }
  }

  private TestEnvironment newTestEnvironment() {
    return new TestEnvironment(DEFAULT_TIMEOUT_MILLIS);
  }

}
