package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.support.TCKVerificationSupport;
import org.reactivestreams.tck.support.TestException;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
* Validates that the TCK's {@link org.reactivestreams.tck.PublisherVerification} fails with nice human readable errors.
* <b>Important: Please note that all Publishers implemented in this file are *wrong*!</b>
*/
public class PublisherVerificationTest extends TCKVerificationSupport {

  @Test
  public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements_shouldFailBy_ExpectingOnError() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements();
      }
    }, "produced no element after first");
  }

  @Test
  public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription_shouldFailBy_notReceivingAnyElement() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().required_spec102_maySignalLessThanRequestedAndTerminateSubscription();
      }
    }, "Did not receive expected element");
  }

  @Test
  public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription_shouldFailBy_receivingTooManyElements() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        demandIgnoringSynchronousPublisherVerification().required_spec102_maySignalLessThanRequestedAndTerminateSubscription();
      }
    }, "Expected end-of-stream but got element [3]");
  }

  @Test
  public void stochastic_spec103_mustSignalOnMethodsSequentially_shouldFailBy_concurrentlyAccessingOnNext() throws Throwable {
    final AtomicInteger startedSignallingThreads = new AtomicInteger(0);
    // this is an arbitrary number, we just need "many threads" to try to force an concurrent access scenario
    final int maxSignallingThreads = 10;

    final ExecutorService signallersPool = Executors.newFixedThreadPool(maxSignallingThreads);
    final AtomicBoolean concurrentAccessCaused = new AtomicBoolean(false);

    // highly specialised threadpool driven publisher which aims to FORCE concurrent access,
    // so that we can confirm the test is able to catch this.
    final Publisher<Integer> concurrentAccessPublisher = new Publisher<Integer>() {
      @Override public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new NoopSubscription() {
          @Override public void request(final long n) {
            Runnable signalling = new Runnable() {

              @Override public void run() {
                for (long i = 0; i < n; i++) {
                  try {
                    // shutdown cleanly in when the threadpool is shutting down
                    if (Thread.interrupted()) {
                      return;
                    }

                    s.onNext((int) i);
                  } catch (Exception ex) {
                    // signal others to shut down
                    signallersPool.shutdownNow();

                    if (ex instanceof TestEnvironment.Latch.ExpectedOpenLatchException) {
                      if (!concurrentAccessCaused.getAndSet(true)) {
                        throw new RuntimeException("Concurrent access detected", ex);
                      } else {
                        // error signalled once already, stop more errors from propagating
                        return;
                      }
                    } else {
                      throw new RuntimeException(ex);
                    }
                  }
                }
              }
            };

            // must be guarded like this in case a Subscriber triggers request() synchronously from it's onNext()
            while (startedSignallingThreads.getAndAdd(1) < maxSignallingThreads && !signallersPool.isShutdown()) {
              try {
                signallersPool.execute(signalling);
              } catch (RejectedExecutionException ex) {
                // ignore, should be safe as it means the pool is shutting down -> which means we triggered the problem we wanted to
                return;
              }
            }
          }
        });
      }
    };

    try {
      requireTestFailure(new ThrowingRunnable() {
        @Override public void run() throws Throwable {
          customPublisherVerification(concurrentAccessPublisher).stochastic_spec103_mustSignalOnMethodsSequentially();
        }
      }, "Illegal concurrent access detected");
    } finally {
      signallersPool.shutdownNow();
      signallersPool.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test
  public void stochastic_spec103_mustSignalOnMethodsSequentially_shouldPass_forSynchronousPublisher() throws Throwable {
    customPublisherVerification(new Publisher<Integer>() {
      @Override public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new NoopSubscription() {
          int element = 0;
          @Override public void request(long n) {
            for (int i = 0; i < n; i++) {
              s.onNext(element++);
            }
            s.onComplete();
          }
        });
      }
    }).stochastic_spec103_mustSignalOnMethodsSequentially();
  }

  @Test
  public void optional_spec104_mustSignalOnErrorWhenFails_shouldFail() throws Throwable {
    final Publisher<Integer> invalidErrorPublisher = new Publisher<Integer>() {
      @Override public void subscribe(Subscriber<? super Integer> s) {
        throw new RuntimeException("It is not valid to throw here!");
      }
    };
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(SKIP, invalidErrorPublisher).optional_spec104_mustSignalOnErrorWhenFails();
      }
    }, "Publisher threw exception (It is not valid to throw here!) instead of signalling error via onError!");
  }

  @Test
  public void optional_spec104_mustSignalOnErrorWhenFails_shouldBeSkippedWhenNoErrorPublisherGiven() throws Throwable {
    requireTestSkip(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().optional_spec104_mustSignalOnErrorWhenFails();
      }
    }, PublisherVerification.SKIPPING_NO_ERROR_PUBLISHER_AVAILABLE);
  }

  @Test
  public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates_shouldFail() throws Throwable {
    final Publisher<Integer> forgotToSignalCompletionPublisher = new Publisher<Integer>() {
      @Override public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new NoopSubscription() {
          int signal = 0;

          @Override public void request(long n) {
            for (int i = 0; i < n; i++) {
              s.onNext(signal);
              signal += 1;
            }
            // intentional omission of onComplete
          }
        });
      }
    };

    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(forgotToSignalCompletionPublisher).required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates();
      }
    }, "Expected end-of-stream but got element [3]");
  }

  @Test
  public void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete_shouldNotAllowEagerOnComplete() throws Throwable {
    final Publisher<Integer> illegalEmptyEagerOnCompletePublisher = new Publisher<Integer>() {
      @Override public void subscribe(final Subscriber<? super Integer> s) {
        s.onComplete();
      }
    };

    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        PublisherVerification<Integer> verification = new PublisherVerification<Integer>(newTestEnvironment()) {
          @Override public Publisher<Integer> createPublisher(long elements) {
            return illegalEmptyEagerOnCompletePublisher;
          }

          @Override public long maxElementsFromPublisher() {
            return 0; // it is an "empty" Publisher
          }

          @Override public Publisher<Integer> createErrorStatePublisher() {
            return null;
          }
        };

        verification.optional_spec105_emptyStreamMustTerminateBySignallingOnComplete();
      }
    }, "Subscriber::onComplete() called before Subscriber::onSubscribe");
  }

  @Test
  public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled_shouldFailForNotCompletingPublisher() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        demandIgnoringSynchronousPublisherVerification().required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
      }
    }, "Expected end-of-stream but got element [" /* element which should not have been signalled */);
  }

  @Test
  public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled_shouldFailForPublisherWhichCompletesButKeepsServingData() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new NoopSubscription() {

              boolean completed = false;

              @Override public void request(long n) {
                // emit one element
                s.onNext(0);

                // and "complete"
                // but keep signalling data if more demand comes in anyway!
                if (!completed) {
                  s.onComplete();
                }

              }
            });
          }
        }).required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
      }
    }, "Unexpected element 0 received after stream completed");
  }

  @Test
  public void optional_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe_actuallyPass() throws Throwable {
    customPublisherVerification(SKIP, new Publisher<Integer>() {
      @Override public void subscribe(Subscriber<? super Integer> s) {
        s.onError(new RuntimeException("Sorry, I'm busy now. Call me later."));
      }
    }).required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe();
  }

  @Test
  public void optional_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe_shouldFail() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(SKIP, new Publisher<Integer>() {
          @Override public void subscribe(Subscriber<? super Integer> s) {
          }
        }).required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe();
      }
    }, "Should have received onError");
  }

  @Test
  public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe_beSkippedForNoGivenErrorPublisher() throws Throwable {
    requireTestSkip(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe();
      }
    }, PublisherVerification.SKIPPING_NO_ERROR_PUBLISHER_AVAILABLE);
  }

  @Test
  public void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice_shouldFailBy_skippingSinceOptional() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice();
      }
    }, "Not verified by this TCK.");
  }

  @Test
  public void optional_spec111_maySupportMultiSubscribe_shouldFailBy_actuallyPass() throws Throwable {
    noopPublisherVerification().optional_spec111_maySupportMultiSubscribe();
  }

  @Test
  public void required_spec112_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront_shouldFailBy_expectingOnError() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new Subscription() {
              final Random rnd = new Random();
              @Override public void request(long n) {
                for (int i = 0; i < n; i++) {
                  s.onNext(rnd.nextInt());
                }
              }

              @Override public void cancel() {

              }
            });
          }
        }).required_spec112_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
      }
    }, "Expected elements to be signaled in the same sequence to 1st and 2nd subscribers: Lists differ at element ");
  }


  @Test
  public void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe_shouldFailBy_reportingAsyncError() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        onErroringPublisherVerification().required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe();
      }
    }, "Async error during test execution: Test Exception: Boom!");
  }

  @Test
  public void required_spec303_mustNotAllowUnboundedRecursion_shouldFailBy_informingAboutTooDeepStack() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new Subscription() {
              @Override public void request(long n) {
                s.onNext(0); // naive reccursive call, would explode with StackOverflowException
              }

              @Override public void cancel() {
                // noop
              }
            });
          }
        }).required_spec303_mustNotAllowUnboundedRecursion();
      }
    }, /* Got 2 onNext calls within thread: ... */ "yet expected recursive bound was 1");
  }

  @Test
  public void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops_shouldFailBy_unexpectedElement() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        demandIgnoringSynchronousPublisherVerification().required_spec306_afterSubscriptionIsCancelledRequestMustBeNops();
      }
    }, "Did not expect an element but got element [0]");
  }

  @Test
  public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops_shouldPass() throws Throwable {
    demandIgnoringSynchronousPublisherVerification().required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
  }

  @Test
  public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops_shouldFailBy_unexpectedErrorInCancelling() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new Subscription() {
              @Override public void request(long n) {
                // noop
              }

              @Override public void cancel() {
                s.onError(new TestException()); // illegal error signalling!
              }
            });
          }
        }).required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
      }
    }, "Async error during test execution: Test Exception: Boom!");
  }

  @Test
  public void required_spec309_requestZeroMustSignalIllegalArgumentException_shouldFailBy_expectingOnError() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().required_spec309_requestZeroMustSignalIllegalArgumentException();
      }
    }, "Expected onError");
  }

  @Test
  public void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException_shouldFailBy_expectingOnError() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        noopPublisherVerification().required_spec309_requestNegativeNumberMustSignalIllegalArgumentException();
      }
    }, "Expected onError");
  }

  @Test
  public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling_shouldFailBy_havingEmitedMoreThanRequested() throws Throwable {
    final ExecutorService pool = Executors.newFixedThreadPool(2);

    try {
      requireTestFailure(new ThrowingRunnable() {
        @Override public void run() throws Throwable {
          demandIgnoringAsynchronousPublisherVerification(pool).required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling();
        }
      }, /*Publisher signalled [...] */ ", which is more than the signalled demand: ");
    } finally {
      pool.shutdownNow();
      pool.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test
  public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber_shouldFailBy_keepingTheReferenceLongerThanNeeded() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          Subscriber subs = null;

          @Override public void subscribe(final Subscriber<? super Integer> s) {
            subs = s; // keep the reference

            s.onSubscribe(new Subscription() {
              @Override public void request(long n) {
                for (int i = 0; i < n; i++) {
                  s.onNext((int) n);
                }
              }

              @Override public void cancel() {
                // noop, we still keep the reference!
              }
            });
          }
        }).required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
      }
    }, "did not drop reference to test subscriber after subscription cancellation");
  }

  @Test
  public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue_shouldFail_onAsynchDemandIgnoringPublisher() throws Throwable {
    // 10 is arbitrary here, we just need a "larger number" to get into concurrent access scenarios, anything more than 2
    // should work, but getting up to 10 should be safer and doesn't hurt to play safe here
    final ExecutorService pool = Executors.newFixedThreadPool(10);

    try {
      requireTestFailure(new ThrowingRunnable() {
        @Override public void run() throws Throwable {
          demandIgnoringAsynchronousPublisherVerification(pool).required_spec317_mustSupportAPendingElementCountUpToLongMaxValue();
        }
      }, "Expected end-of-stream but got");
    } finally {
      pool.shutdownNow();
      pool.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test
  public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue_shouldFail_onSynchDemandIgnoringPublisher() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        demandIgnoringSynchronousPublisherVerification().required_spec317_mustSupportAPendingElementCountUpToLongMaxValue();
      }
    }, "Received more than bufferSize (32) onNext signals. The Publisher probably emited more signals than expected!");
  }

  @Test
  public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue_shouldFail_onSynchOverflowingPublisher() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          long demand = 0;

          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new Subscription() {
              @Override public void request(long n) {
                // it does not protect from demand overflow!
                demand += n;
                if (demand < 0) {
                  // overflow
                  s.onError(new IllegalStateException("Illegally signalling onError (violates rule 3.17)")); // Illegally signal error
                } else {
                  s.onNext(0);
                }
              }

              @Override public void cancel() {
                // noop
              }
            });
          }
        }).required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue();
      }
    }, "Async error during test execution: Illegally signalling onError (violates rule 3.17)");
  }

  @Test
  public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue_shouldFailWhenErrorSignalledOnceMaxValueReached() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        customPublisherVerification(new Publisher<Integer>() {
          long demand = 0;

          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new NoopSubscription() {
              @Override public void request(long n) {
                demand += n;

                // this is a mistake, it should still be able to accumulate such demand
                if (demand == Long.MAX_VALUE)
                  s.onError(new IllegalStateException("Illegally signalling onError too soon! " +
                                                          "Cumulative demand equal to Long.MAX_VALUE is legal."));

                s.onNext(0);
              }
            });
          }
        }).required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
      }
    }, "Async error during test execution: Illegally signalling onError too soon!");
  }


  // FAILING IMPLEMENTATIONS //

  final Publisher<Integer> SKIP = null;

  /** Subscription which does nothing. */
  static class NoopSubscription implements Subscription {

    @Override public void request(long n) {
      // noop
    }

    @Override public void cancel() {
      // noop
    }
  }

  /**
   * Verification using a Publisher that never publishes any element.
   * Skips the error state publisher tests.
   */
  final PublisherVerification<Integer> noopPublisherVerification() {
    return new PublisherVerification<Integer>(newTestEnvironment()) {
      @Override public Publisher<Integer> createPublisher(long elements) {

        return new Publisher<Integer>() {
          @Override public void subscribe(Subscriber<? super Integer> s) {
            s.onSubscribe(new NoopSubscription());
          }
        };

      }

      @Override public Publisher<Integer> createErrorStatePublisher() {
        return SKIP;
      }
    };
  }

  /**
   * Verification using a Publisher that never publishes any element
   */
  final PublisherVerification<Integer> onErroringPublisherVerification() {
    return new PublisherVerification<Integer>(newTestEnvironment()) {
      @Override public Publisher<Integer> createPublisher(long elements) {

        return new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new NoopSubscription() {
              @Override public void request(long n) {
                s.onError(new TestException());
              }
            });
          }
        };

      }

      @Override public Publisher<Integer> createErrorStatePublisher() {
        return SKIP;
      }
    };
  }

  /**
   * Custom Verification using given Publishers
   */
  final PublisherVerification<Integer> customPublisherVerification(final Publisher<Integer> pub) {
    return customPublisherVerification(pub, SKIP);
  }

  /**
   * Custom Verification using given Publishers
   */
  final PublisherVerification<Integer> customPublisherVerification(final Publisher<Integer> pub, final Publisher<Integer> errorPub) {
    return new PublisherVerification<Integer>(newTestEnvironment()) {
      @Override public Publisher<Integer> createPublisher(long elements) {
        return pub;
      }

      @Override public Publisher<Integer> createErrorStatePublisher() {
        return errorPub;
      }
    };
  }

  /**
   * Verification using a Publisher that publishes elements even with no demand available
   */
  final PublisherVerification<Integer> demandIgnoringSynchronousPublisherVerification() {
    return new PublisherVerification<Integer>(newTestEnvironment()) {
      @Override public Publisher<Integer> createPublisher(long elements) {

        return new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new NoopSubscription() {
              @Override public void request(long n) {

                for (long i = 0; i <= n; i++) {
                  // one too much
                  s.onNext((int) i);
                }
              }
            });
          }
        };

      }

      @Override public Publisher<Integer> createErrorStatePublisher() {
        return SKIP;
      }
    };
  }

  /**
   * Verification using a Publisher that publishes elements even with no demand available, from multiple threads (!).
   *
   * Please note that exceptions thrown from onNext *will be swallowed* – reason being this verification is used to check
   * very specific things about error reporting - from the "TCK Tests", we do not have any assertions on thrown exceptions.
   */
  final PublisherVerification<Integer> demandIgnoringAsynchronousPublisherVerification(final ExecutorService signallersPool) {
    return demandIgnoringAsynchronousPublisherVerification(signallersPool, true);
  }

  /**
   * Verification using a Publisher that publishes elements even with no demand available, from multiple threads (!).
   */
  final PublisherVerification<Integer> demandIgnoringAsynchronousPublisherVerification(final ExecutorService signallersPool, final boolean swallowOnNextExceptions) {
    final AtomicInteger startedSignallingThreads = new AtomicInteger(0);
    final int maxSignallingThreads = 2;

    final AtomicBoolean concurrentAccessCaused = new AtomicBoolean(false);

    return new PublisherVerification<Integer>(newTestEnvironment()) {
      @Override public Publisher<Integer> createPublisher(long elements) {

        return new Publisher<Integer>() {
          @Override public void subscribe(final Subscriber<? super Integer> s) {
            s.onSubscribe(new NoopSubscription() {
              @Override public void request(final long n) {
                Runnable signalling = new Runnable() {

                  @Override public void run() {
                    for (long i = 0; i <= n; i++) {
                      // one signal too much

                      try {
                        final long signal = i;
                        signallersPool.execute(new Runnable() {
                          @Override public void run() {
                            try {
                              s.onNext((int) signal);
                            } catch (Exception ex) {
                              if (!swallowOnNextExceptions) {
                                throw new RuntimeException("onNext threw an exception!", ex);
                              } else {
                                // yes, swallow the exception, we're not asserting and they'd just end up being logged (stdout),
                                // which we do not need in this specific PublisherVerificationTest
                              }
                            }
                          }
                        });
                      } catch (Exception ex) {
                        if (ex instanceof TestEnvironment.Latch.ExpectedOpenLatchException) {
                          if (concurrentAccessCaused.compareAndSet(false, true)) {
                            throw new RuntimeException("Concurrent access detected", ex);
                          } else {
                            // error signalled once already, stop more errors from propagating
                            return;
                          }
                        } else if (ex instanceof RejectedExecutionException) {
                          // ignore - this may happen since one thread may have already gotten into a concurrent access
                          // problem and initiated the pool's shutdown. It will then throw RejectedExecutionException.
                        } else {
                          if (concurrentAccessCaused.get()) {
                            return;
                          } else {
                            throw new RuntimeException(ex);
                          }
                        }
                      }
                    }
                  }
                };

                // must be guarded like this in case a Subscriber triggers request() synchronously from it's onNext()
                while (startedSignallingThreads.getAndAdd(1) < maxSignallingThreads) {
                  signallersPool.execute(signalling);
                }
              }
            });
          }
        };

      }

      @Override public Publisher<Integer> createErrorStatePublisher() {
        return SKIP;
      }
    };
  }

  private TestEnvironment newTestEnvironment() {
    return new TestEnvironment();
  }


}
