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

package org.reactivestreams.example.unicast;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncSubscriberWhiteboxTest extends SubscriberWhiteboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncSubscriberWhiteboxTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
    return new SyncSubscriber<Integer>() {
      @Override
      public void onSubscribe(final Subscription s) {
        super.onSubscribe(s);

        probe.registerOnSubscribe(new SubscriberPuppet() {
          @Override
          public void triggerRequest(long elements) {
            s.request(elements);
          }

          @Override
          public void signalCancel() {
            s.cancel();
          }
        });
      }

      @Override
      public void onNext(Integer element) {
        super.onNext(element);
        probe.registerOnNext(element);
      }

      @Override
      public void onError(Throwable cause) {
        super.onError(cause);
        probe.registerOnError(cause);
      }

      @Override
      public void onComplete() {
        super.onComplete();
        probe.registerOnComplete();
      }

      @Override
      protected boolean whenNext(Integer element) {
        return true;
      }
    };
  }

  @Override public Integer createElement(int element) {
    return element;
  }

}
