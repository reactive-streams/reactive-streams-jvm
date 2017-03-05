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
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.reactivestreams.tck.support.SyncTriggeredDemandSubscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncTriggeredDemandSubscriberTest extends SubscriberBlackboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncTriggeredDemandSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override public void triggerRequest(final Subscriber<? super Integer> subscriber) {
    ((SyncTriggeredDemandSubscriber<? super Integer>)subscriber).triggerDemand(1);
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
}
