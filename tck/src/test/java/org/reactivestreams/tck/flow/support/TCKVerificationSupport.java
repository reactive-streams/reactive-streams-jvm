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

package org.reactivestreams.tck.flow.support;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.SkipException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.fail;

/**
 * Provides assertions to validate the TCK tests themselves,
 * with the goal of guaranteeing proper error messages when an implementation does not pass a given TCK test.
 *
 * "Quis custodiet ipsos custodes?" -- Iuvenalis
 */
public class TCKVerificationSupport {

  // INTERNAL ASSERTION METHODS //

  /**
   * Runs given code block and expects it to fail with an "Expected onError" failure.
   * Use this method to validate that TCK tests fail with meaningful errors instead of NullPointerExceptions etc.
   *
   * @param run     encapsulates test case which we expect to fail
   * @param msgPart the exception failing the test (inside the run parameter) must contain this message part in one of it's causes
   */
  public void requireTestFailure(ThrowingRunnable run, String msgPart) {
    try {
      run.run();
    } catch (Throwable throwable) {
      if (findDeepErrorMessage(throwable, msgPart)) {
        return;
      } else {
        throw new RuntimeException(
          String.format("Expected TCK to fail with '... %s ...', " +
                          "yet `%s(%s)` was thrown and test would fail with not useful error message!",
                        msgPart, throwable.getClass().getName(), throwable.getMessage()), throwable);
      }
    }
    throw new RuntimeException(String.format("Expected TCK to fail with '... %s ...', " +
                                               "yet no exception was thrown and test would pass unexpectedly!",
                                             msgPart));
  }

  /**
   * Runs given code block and expects it fail with an {@code org.testng.SkipException}
   *
   * @param run encapsulates test case which we expect to be skipped
   * @param msgPart the exception failing the test (inside the run parameter) must contain this message part in one of it's causes
   */
  public void requireTestSkip(ThrowingRunnable run, String msgPart) {
    try {
      run.run();
    } catch (SkipException skip) {
        if (skip.getMessage().contains(msgPart)) {
          return;
        } else {
          throw new RuntimeException(
            String.format("Expected TCK to skip this test with '... %s ...', yet it skipped with (%s) instead!",
                          msgPart, skip.getMessage()), skip);
        }
    } catch (Throwable throwable) {
        throw new RuntimeException(
          String.format("Expected TCK to skip this test, yet it threw %s(%s) instead!",
                        throwable.getClass().getName(), throwable.getMessage()), throwable);
    }
    throw new RuntimeException("Expected TCK to SKIP this test, instead if PASSed!");
  }

  /**
   * This publisher does NOT fulfil all Publisher spec requirements.
   * It's just the bare minimum to enable this test to fail the Subscriber tests.
   */
  public Publisher<Integer> newSimpleIntsPublisher(final long maxElementsToEmit) {
    return new Publisher<Integer>() {
      @Override public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new Subscription() {
          private AtomicLong nums = new AtomicLong();
          private AtomicBoolean active = new AtomicBoolean(true);

          @Override public void request(long n) {
            long thisDemand = n;
            while (active.get() && thisDemand > 0 && nums.get() < maxElementsToEmit) {
              s.onNext((int) nums.getAndIncrement());
              thisDemand--;
            }

            if (nums.get() == maxElementsToEmit) {
              s.onComplete();
            }
          }

          @Override public void cancel() {
            active.set(false);
          }
        });
      }
    };
  }

  /**
   * Looks for expected error message prefix inside of causes of thrown throwable.
   *
   * @return true if one of the causes indeed contains expected error, false otherwise
   */
  public boolean findDeepErrorMessage(Throwable throwable, String msgPart) {
    return findDeepErrorMessage(throwable, msgPart, 5);
  }

  private boolean findDeepErrorMessage(Throwable throwable, String msgPart, int depth) {
    if (throwable instanceof NullPointerException) {
      fail(String.format("%s was thrown, definitely not a helpful error!", NullPointerException.class.getName()), throwable);
      return false;
    } else if (throwable == null || depth == 0) {
      return false;
    } else {
      final String message = throwable.getMessage();
      return (message != null && message.contains(msgPart)) || findDeepErrorMessage(throwable.getCause(), msgPart, depth - 1);
    }
  }

  /** Like {@link java.lang.Runnable} but allows throwing {@link java.lang.Throwable}, useful for wrapping test execution */
  public static interface ThrowingRunnable {
    void run() throws Throwable;
  }
}
