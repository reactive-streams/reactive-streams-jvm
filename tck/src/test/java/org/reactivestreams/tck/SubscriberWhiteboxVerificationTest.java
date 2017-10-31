/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

package org.reactivestreams.tck;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification.SubscriberPuppet;
import org.reactivestreams.tck.SubscriberWhiteboxVerification.WhiteboxSubscriberProbe;
import org.reactivestreams.tck.flow.support.Function;
import org.reactivestreams.tck.flow.support.TCKVerificationSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Validates that the TCK's {@link SubscriberWhiteboxVerification} fails with nice human readable errors.
 * <b>Important: Please note that all Subscribers implemented in this file are *wrong*!</b>
 */
public class SubscriberWhiteboxVerificationTest extends TCKVerificationSupport {

  private ExecutorService ex;
  @BeforeClass void before() { ex = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (ex != null) ex.shutdown(); }

  @Test
  public void required_spec201_mustSignalDemandViaSubscriptionRequest_shouldFailBy_notGettingRequestCall() throws Throwable {
    // this mostly verifies the probe is injected correctly
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override public Subscriber<Integer> apply(final WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(final Subscription s) {
                probe.registerOnSubscribe(new SubscriberPuppet() {
                  @Override public void triggerRequest(long elements) {
                    // forgot to implement request triggering properly!
                  }

                  @Override public void signalCancel() {
                    s.cancel();
                  }
                });
              }
            };
          }
        }).required_spec201_mustSignalDemandViaSubscriptionRequest();
      }
    }, "Did not receive expected `request` call within");
  }

  @Test
  public void required_spec201_mustSignalDemandViaSubscriptionRequest_shouldPass() throws Throwable {
    simpleSubscriberVerification().required_spec201_mustSignalDemandViaSubscriptionRequest();
  }

  @Test
  public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete_shouldFail_dueToCallingRequest() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override public Subscriber<Integer> apply(final WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(final Subscription s) {
                this.subscription = s;
                probe.registerOnSubscribe(newSimpleSubscriberPuppet(s));
              }

              @Override public void onComplete() {
                subscription.request(1);
                probe.registerOnComplete();
              }
            };
          }
        }).required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
      }
    }, "Subscription::request MUST NOT be called from Subscriber::onComplete (Rule 2.3)!");
  }

  @Test
  public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete_shouldFail_dueToCallingCancel() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override public Subscriber<Integer> apply(final WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(final Subscription s) {
                this.subscription = s;
                probe.registerOnSubscribe(newSimpleSubscriberPuppet(s));
              }

              @Override public void onComplete() {
                subscription.cancel();
                probe.registerOnComplete();
              }
            };
          }
        }).required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
      }
    }, "Subscription::cancel MUST NOT be called from Subscriber::onComplete (Rule 2.3)!");
  }

  @Test
  public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError_shouldFail_dueToCallingRequest() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override public Subscriber<Integer> apply(final WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(final Subscription s) {
                this.subscription = s;
                probe.registerOnSubscribe(newSimpleSubscriberPuppet(s));
              }

              @Override public void onError(Throwable t) {
                subscription.request(1);
              }
            };
          }
        }).required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
      }
    }, "Subscription::request MUST NOT be called from Subscriber::onError (Rule 2.3)!");
  }

  @Test
  public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError_shouldFail_dueToCallingCancel() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override public Subscriber<Integer> apply(final WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(final Subscription s) {
                this.subscription = s;
                probe.registerOnSubscribe(newSimpleSubscriberPuppet(s));
              }

              @Override public void onError(Throwable t) {
                subscription.cancel();
              }
            };
          }
        }).required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
      }
    }, "Subscription::cancel MUST NOT be called from Subscriber::onError (Rule 2.3)!");
  }

  @Test
  public void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override
          public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(Subscription s) {
                super.onSubscribe(s);

                s.request(1); // this is wrong, as one should always check if should accept or reject the subscription
              }
            };
          }
        }).required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
      }
    }, "Expected 2nd Subscription given to subscriber to be cancelled");
  }

  @Test
  public void required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override
          public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {

            final AtomicBoolean subscriptionCancelled = new AtomicBoolean(false);

            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(final Subscription s) {
                this.subscription = s;
                probe.registerOnSubscribe(new SubscriberPuppet() {
                  @Override public void triggerRequest(long elements) {
                    s.request(elements);
                  }

                  @Override public void signalCancel() {
                    subscriptionCancelled.set(true);
                    s.cancel();
                  }
                });
              }

              @Override public void onNext(Integer element) {
                if (subscriptionCancelled.get()) {
                  // this is wrong for many reasons, firstly onNext should never throw,
                  // but this test aims to simulate a Subscriber where someone got it's internals wrong and "blows up".
                  throw new RuntimeException("But I thought it's cancelled!");
                } else {
                  probe.registerOnNext(element);
                }
              }
            };
          }
        }).required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel();
      }
    }, "But I thought it's cancelled!");
  }

  @Test
  public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override
          public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onComplete() {
                // forgot to call the probe here
              }
            };
          }
        }).required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
      }
    }, "did not call `registerOnComplete()`");
  }

  @Test
  public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall_shouldPass_withNoopSubscriber() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override
          public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(Subscription s) {
                // intentional omission of probe registration
              }
            };
          }
        }).required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();

      }
    }, "did not `registerOnSubscribe`");
  }

  @Test
  public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {

        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override
          public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onError(Throwable t) {
                // this is wrong in many ways (incl. spec violation), but aims to simulate user code which "blows up" when handling the onError signal
                throw new RuntimeException("Wrong, don't do this!", t); // intentional spec violation
              }
            };
          }
        }).required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
      }
    }, "Test Exception: Boom!"); // checks that the expected exception was delivered to onError, we don't expect anyone to implement onError so weirdly
  }

  @Test
  public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {

        customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
          @Override
          public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
            return new SimpleSubscriberWithProbe(probe) {
              @Override public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
              }

              @Override public void onError(Throwable t) {
                // this is wrong in many ways (incl. spec violation), but aims to simulate user code which "blows up" when handling the onError signal
                throw new RuntimeException("Wrong, don't do this!", t);
              }
            };
          }
        }).required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall();
      }
    }, "Test Exception: Boom!"); // checks that the expected exception was delivered to onError, we don't expect anyone to implement onError so weirdly
  }
  
  @Test
  public void required_spec308_requestMustRegisterGivenNumberElementsToBeProduced_shouldFail() throws Throwable {
    // sanity checks the "happy path", that triggerRequest() propagates the right demand
    customSubscriberVerification(new Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>>() {
      @Override
      public Subscriber<Integer> apply(WhiteboxSubscriberProbe<Integer> probe) throws Throwable {
        return new SimpleSubscriberWithProbe(probe) {};
      }
    }).required_spec308_requestMustRegisterGivenNumberElementsToBeProduced();
  }

  // FAILING IMPLEMENTATIONS //

  /**
   * Verification using a Subscriber that doesn't do anything on any of the callbacks.
   *
   * The {@link org.reactivestreams.tck.SubscriberWhiteboxVerification.WhiteboxSubscriberProbe} is properly installed in this subscriber.
   *
   * This verification can be used in the "simples case, subscriber which does basically nothing case" validation.
   */
  final SubscriberWhiteboxVerification<Integer> simpleSubscriberVerification() {
    return new SubscriberWhiteboxVerification<Integer>(newTestEnvironment()) {
      @Override
      public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
        return new Subscriber<Integer>() {
          @Override public void onSubscribe(final Subscription s) {
            probe.registerOnSubscribe(new SubscriberPuppet() {
              @Override public void triggerRequest(long elements) {
                s.request(elements);
              }

              @Override public void signalCancel() {
                s.cancel();
              }
            });
          }

          @Override public void onNext(Integer element) {
            probe.registerOnNext(element);
          }

          @Override public void onError(Throwable t) {
            probe.registerOnError(t);
          }

          @Override public void onComplete() {
            probe.registerOnComplete();
          }
        };
      }

      @Override public Integer createElement(int element) { return element; }

      @Override public ExecutorService publisherExecutorService() { return ex; }
    };
  }

  /**
   * Verification using a Subscriber that can be fine tuned by the TCK implementer
   */
  final SubscriberWhiteboxVerification<Integer> customSubscriberVerification(final Function<WhiteboxSubscriberProbe<Integer>, Subscriber<Integer>> newSubscriber) {
    return new SubscriberWhiteboxVerification<Integer>(newTestEnvironment()) {
      @Override
      public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        try {
          return newSubscriber.apply(probe);
        } catch (Throwable t) {
          throw new RuntimeException("Unable to create subscriber!", t);
        }
      }

      @Override public Integer createElement(int element) { return element; }

      @Override public ExecutorService publisherExecutorService() { return ex; }
    };
  }

  private SubscriberPuppet newSimpleSubscriberPuppet(final Subscription subscription) {
    return new SubscriberPuppet() {
      @Override public void triggerRequest(long elements) {
        subscription.request(elements);
      }

      @Override public void signalCancel() {
        subscription.cancel();
      }
    };
  }

  /**
   * Simplest possible implementation of Subscriber which calls the WhiteboxProbe in all apropriate places.
   * Override it to save some lines of boilerplate, and then break behaviour in specific places.
   */
  private abstract class SimpleSubscriberWithProbe implements Subscriber<Integer> {

    volatile Subscription subscription;

    final WhiteboxSubscriberProbe<Integer> probe;

    public SimpleSubscriberWithProbe(WhiteboxSubscriberProbe<Integer> probe) {
      this.probe = probe;
    }

    @Override public void onSubscribe(final Subscription s) {
      this.subscription = s;
      probe.registerOnSubscribe(newSimpleSubscriberPuppet(s));
    }

    @Override public void onNext(Integer element) {
      probe.registerOnNext(element);
    }

    @Override public void onError(Throwable t) {
      probe.registerOnError(t);
    }

    @Override public void onComplete() {
      probe.registerOnComplete();
    }
  }

  private TestEnvironment newTestEnvironment() {
    return new TestEnvironment();
  }

}
