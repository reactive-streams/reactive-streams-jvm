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
import org.reactivestreams.tck.flow.support.TCKVerificationSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
      @Override public void run() throws Throwable {
        new IdentityProcessorVerification<Integer>(newTestEnvironment(), DEFAULT_TIMEOUT_MILLIS){
          @Override public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return new NoopProcessor();
          }

          @Override public ExecutorService publisherExecutorService() { return ex; }

          @Override public Integer createElement(int element) { return element; }

          @Override public Publisher<Integer> createHelperPublisher(long elements) {
            return SKIP;
          }

          @Override public Publisher<Integer> createFailedPublisher() {
            return SKIP;
          }

          @Override public long maxSupportedSubscribers() {
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
