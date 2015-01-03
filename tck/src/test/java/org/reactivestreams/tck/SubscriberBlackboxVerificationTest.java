package org.reactivestreams.tck;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.support.TCKVerificationSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
* Validates that the TCK's {@link org.reactivestreams.tck.SubscriberBlackboxVerification} fails with nice human readable errors.
* <b>Important: Please note that all Subscribers implemented in this file are *wrong*!</b>
*/
public class SubscriberBlackboxVerificationTest extends TCKVerificationSupport {

  private ExecutorService ex;
  @BeforeClass void before() { ex = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (ex != null) ex.shutdown(); }

  @Test
  public void required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest_shouldFailBy_notGettingRequestCall() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopSubscriberVerification().required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest();
      }
    }, "Did not receive expected `request` call within");
  }

  @Test
  public void required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest_shouldPass() throws Throwable {
    simpleSubscriberVerification().required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest();
  }

  @Test
  public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete_shouldFail_dueToCallingRequest() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onComplete() {
            subscription.request(1);
          }
        }).required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
      }
    }, "Subscription::request MUST NOT be called from Subscriber::onComplete (Rule 2.3)!");
  }

  @Test
  public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete_shouldFail_dueToCallingCancel() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onComplete() {
            subscription.cancel();
          }
        }).required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
      }
    }, "Subscription::cancel MUST NOT be called from Subscriber::onComplete (Rule 2.3)!");
  }

  @Test
  public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError_shouldFail_dueToCallingRequest() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onError(Throwable t) {
            subscription.request(1);
          }
        }).required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
      }
    }, "Subscription::request MUST NOT be called from Subscriber::onError (Rule 2.3)!");
  }
  @Test
  public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError_shouldFail_dueToCallingCancel() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onError(Throwable t) {
            subscription.cancel();
          }
        }).required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
      }
    }, "Subscription::cancel MUST NOT be called from Subscriber::onError (Rule 2.3)!");
  }

  @Test
  public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new KeepSubscriptionSubscriber() {
          @Override public void onSubscribe(Subscription s) {
            super.onSubscribe(s);

            s.request(1); // this is wrong, as one should always check if should accept or reject the subscription
          }
        }).required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
      }
    }, "illegally called `subscription.request(1)");
  }

  @Test
  public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new NoopSubscriber() {
          // don't even request()
        }).required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
      }
    }, "did not call `registerOnComplete()`");
  }

  @Test
  public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall_shouldPass_withNoopSubscriber() throws Throwable {
    customSubscriberVerification(new NoopSubscriber() {
      // don't even request()
    }).required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
  }

  @Test
  public void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {

        customSubscriberVerification(new NoopSubscriber() {
          @Override public void onError(Throwable t) {
            // this is wrong in many ways (incl. spec violation), but aims to simulate user code which "blows up" when handling the onError signal
            throw new RuntimeException("Wrong, don't do this!", t); // don't do this
          }
        }).required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
      }
    }, "Test Exception: Boom!"); // checks that the expected exception was delivered to onError, we don't expect anyone to implement onError so weirdly
  }

  // FAILING IMPLEMENTATIONS //

  /**
   * Verification using a Subscriber that doesn't do anything on any of the callbacks
   */
  final SubscriberBlackboxVerification<Integer> noopSubscriberVerification() throws Exception {
    return new SubscriberBlackboxVerification<Integer>(newTestEnvironment()) {
      @Override public Subscriber<Integer> createSubscriber() {
        return new NoopSubscriber();
      }

      @Override public Integer createElement(int element) {
        return element;
      }

      @Override public ExecutorService publisherExecutorService() { return ex; }
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

      @Override public Integer createElement(int element) { return element; }

      @Override public ExecutorService publisherExecutorService() { return ex; }
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

      @Override public Integer createElement(int element) { return element; }

      @Override public ExecutorService publisherExecutorService() { return ex; }
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
    return new TestEnvironment();
  }

}
