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

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.support.NonFatal;
import org.reactivestreams.tck.support.TCKVerificationSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
* Validates that the TCK's {@link IdentityProcessorVerification} fails with nice human readable errors.
* <b>Important: Please note that all Processors implemented in this file are *wrong*!</b>
*/
public class IdentityProcessorVerificationTest extends TCKVerificationSupport {

  static final long DEFAULT_TIMEOUT_MILLIS = TestEnvironment.envDefaultTimeoutMillis();
  static final long DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS = TestEnvironment.envDefaultNoSignalsTimeoutMillis();

  private ExecutorService ex;
  @BeforeClass void before() { ex = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (ex != null) ex.shutdown(); }

  @Test
  public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError_shouldBeIgnored() throws Throwable {
    requireTestSkip(new ThrowingRunnable() {
      @Override
      public void run() throws Throwable {
        new IdentityProcessorVerification<Integer>(newTestEnvironment(), DEFAULT_TIMEOUT_MILLIS) {
          @Override
          public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return new NoopProcessor();
          }

          @Override
          public ExecutorService publisherExecutorService() {
            return ex;
          }

          @Override
          public Integer createElement(int element) {
            return element;
          }

          @Override
          public Publisher<Integer> createHelperPublisher(long elements) {
            return SKIP;
          }

          @Override
          public Publisher<Integer> createFailedPublisher() {
            return SKIP;
          }

          @Override
          public long maxSupportedSubscribers() {
            return 1; // can only support 1 subscribe => unable to run this test
          }
        }.required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError();
      }
    }, "The Publisher under test only supports 1 subscribers, while this test requires at least 2 to run");
  }

  @Test
  public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError_shouldFailWhileWaitingForOnError() throws Throwable {
    requireTestFailure(new ThrowingRunnable() {
      @Override public void run() throws Throwable {
        new IdentityProcessorVerification<Integer>(newTestEnvironment(), DEFAULT_TIMEOUT_MILLIS) {
          @Override public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return new Processor<Integer, Integer>() {
              @Override public void subscribe(final Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {
                  @Override public void request(long n) {
                    s.onNext(0);
                  }

                  @Override public void cancel() {
                  }
                });
              }

              @Override public void onSubscribe(Subscription s) {
                s.request(1);
              }

              @Override public void onNext(Integer integer) {
                // noop
              }

              @Override public void onError(Throwable t) {
                // noop
              }

              @Override public void onComplete() {
                // noop
              }
            };
          }

          @Override public ExecutorService publisherExecutorService() { return ex; }

          @Override public Integer createElement(int element) { return element; }

          @Override public Publisher<Integer> createHelperPublisher(long elements) {
            return new Publisher<Integer>() {
              @Override public void subscribe(final Subscriber<? super Integer> s) {
                s.onSubscribe(new NoopSubscription() {
                  @Override public void request(long n) {
                    for (int i = 0; i < 10; i++) {
                      s.onNext(i);
                    }
                  }
                });
              }
            };
          }

          @Override public Publisher<Integer> createFailedPublisher() {
            return SKIP;
          }
        }.required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError();
      }
    }, "Did not receive expected error on downstream within " + DEFAULT_TIMEOUT_MILLIS);
  }

  @Test
  public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError_shouldAllowSignalingElementAfterBothDownstreamsDemand() throws Throwable {
    final TestEnvironment env = newTestEnvironment();
    new IdentityProcessorVerification<Integer>(env, DEFAULT_TIMEOUT_MILLIS) {
      @Override
      public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) { // knowingly ignoring buffer size, acting as-if 0
        return new Processor<Integer, Integer>() {

          private volatile Subscription upstreamSubscription;

          private final CopyOnWriteArrayList<MySubscription> subs = new CopyOnWriteArrayList<MySubscription>();
          private final CopyOnWriteArrayList<Subscriber<? super Integer>> subscribers = new CopyOnWriteArrayList<Subscriber<? super Integer>>();
          private final AtomicLong demand1 = new AtomicLong();
          private final AtomicLong demand2 = new AtomicLong();
          private final CountDownLatch awaitLatch = new CountDownLatch(2); // to know when both subscribers have signalled demand

          @Override
          public void subscribe(final Subscriber<? super Integer> s) {
            int subscriberCount = subs.size();
            switch (subscriberCount) {
              case 0:
                s.onSubscribe(createSubscription(awaitLatch, s, demand1));
                break;
              case 1:
                s.onSubscribe(createSubscription(awaitLatch, s, demand2));
                break;
              default:
                throw new RuntimeException(String.format("This for-test-purposes-processor supports only 2 subscribers, yet got %s!", subscriberCount));
            }
          }

          @Override
          public void onSubscribe(Subscription s) {
            this.upstreamSubscription = s;
          }

          @Override
          public void onNext(Integer elem) {
            for (Subscriber<? super Integer> subscriber : subscribers) {
              try {
                subscriber.onNext(elem);
              } catch (Throwable t) {
                env.flop(t, String.format("Calling onNext on [%s] should not throw! See https://github.com/reactive-streams/reactive-streams-jvm#2.13", subscriber));
              }
            }
          }

          @Override
          public void onError(Throwable t) {
            for (Subscriber<? super Integer> subscriber : subscribers) {
              try {
                subscriber.onError(t);
              } catch (Exception ex) {
                env.flop(ex, String.format("Calling onError on [%s] should not throw! See https://github.com/reactive-streams/reactive-streams-jvm#2.13", subscriber));
              }
            }
          }

          @Override
          public void onComplete() {
            for (Subscriber<? super Integer> subscriber : subscribers) {
              try {
                subscriber.onComplete();
              } catch (Exception ex) {
                env.flop(ex, String.format("Calling onComplete on [%s] should not throw! See https://github.com/reactive-streams/reactive-streams-jvm#2.13", subscriber));
              }
            }
          }

          private Subscription createSubscription(CountDownLatch awaitLatch, final Subscriber<? super Integer> s, final AtomicLong demand) {
            final MySubscription sub = new MySubscription(awaitLatch, s, demand);
            subs.add(sub);
            subscribers.add(s);
            return sub;
          }

          final class MySubscription implements Subscription {
            private final CountDownLatch awaitLatch;
            private final Subscriber<? super Integer> s;
            private final AtomicLong demand;

            public MySubscription(CountDownLatch awaitTwoLatch, Subscriber<? super Integer> s, AtomicLong demand) {
              this.awaitLatch = awaitTwoLatch;
              this.s = s;
              this.demand = demand;
            }

            @Override
            public void request(final long n) {
              ex.execute(new Runnable() {
                @Override
                public void run() {
                  if (demand.get() >= 0) {
                    demand.addAndGet(n);
                    awaitLatch.countDown();
                    try {
                      awaitLatch.await(env.defaultTimeoutMillis(), TimeUnit.MILLISECONDS);
                      final long d = demand.getAndSet(0);
                      if (d > 0)  upstreamSubscription.request(d);
                    } catch (InterruptedException e) {
                      env.flop(e, "Interrupted while awaiting for all downstreams to signal some demand.");
                    } catch (Throwable t) {
                      env.flop(t, "Subscription#request has thrown an exception, which is illegal!");
                    }
                  } // else cancel was called, do nothing
                }
              });
            }

            @Override
            public void cancel() {
              demand.set(-1); // marks subscription as cancelled
            }

            @Override
            public String toString() {
              return String.format("IdentityProcessorVerificationTest:MySubscription(%s, demand = %s)", s, demand);
            }
          }
        };
      }

      @Override
      public ExecutorService publisherExecutorService() {
        return ex;
      }

      @Override
      public Integer createElement(int element) {
        return element;
      }

      @Override
      public Publisher<Integer> createFailedPublisher() {
        return SKIP;
      }
    }.required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError();
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

  static class NoopProcessor implements Processor<Integer, Integer> {

    @Override public void subscribe(Subscriber<? super Integer> s) {
      s.onSubscribe(new NoopSubscription());
    }

    @Override public void onSubscribe(Subscription s) {
      // noop
    }

    @Override public void onNext(Integer integer) {
      // noop
    }

    @Override public void onError(Throwable t) {
      // noop
    }

    @Override public void onComplete() {
      // noop
    }
  }

  private TestEnvironment newTestEnvironment() {
    return new TestEnvironment(DEFAULT_TIMEOUT_MILLIS, DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS);
  }


}
