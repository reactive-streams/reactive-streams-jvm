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
import org.reactivestreams.tck.flow.support.SyncTriggeredDemandSubscriber;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test // Must be here for TestNG to find and run this, do not remove
public class BrokenExampleWhiteboxTest extends SubscriberBlackboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public BrokenExampleWhiteboxTest() {
    super(new TestEnvironment());
  }

  @Override public void triggerRequest(final Subscriber<? super Integer> subscriber) {
    ((SyncTriggeredDemandSubscriber<?>)subscriber).triggerDemand(1);
  }

  @Override public Subscriber<Integer> createSubscriber() {
    return new SyncTriggeredDemandSubscriber<Integer>() {
      private long acc;
      @Override protected long foreach(final Integer element) {
        acc += element;
        return 1;
      }

      @Override public void onComplete() {
      }
    };
  }

  @Override public Integer createElement(int element) {
    return element;
  }

  public class MySubscriber<T> implements Subscriber<T> {
      private volatile Subscription subscription;

    public boolean triggerDemand(final long n) {
        final Subscription s = subscription;
        if (s == null) return false;
        else {
          try {
            s.request(n);
          } catch(final Throwable t) {
            // Subscription.request is not allowed to throw according to rule 3.16
            (new IllegalStateException(s + " violated the Reactive Streams rule 3.16 by throwing an exception from request.", t)).printStackTrace(System.err);
            return false;
          }
          return true;
        }
      }

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded
      }

      @Override
      public void onNext(T item) {
        System.out.println("Got : " + item);
        subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onComplete() {
        System.out.println("Done");
      }
    }
}
