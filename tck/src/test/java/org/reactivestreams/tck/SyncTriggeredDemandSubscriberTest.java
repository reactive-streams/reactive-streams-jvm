/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck;

import java.util.concurrent.*;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.flow.support.SyncTriggeredDemandSubscriber;
import org.testng.annotations.*;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncTriggeredDemandSubscriberTest extends SubscriberBlackboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncTriggeredDemandSubscriberTest() {
    super(new TestEnvironment());
  }

  @Test(enabled = false) // TestNG tries to inject here but this is a helper method
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
