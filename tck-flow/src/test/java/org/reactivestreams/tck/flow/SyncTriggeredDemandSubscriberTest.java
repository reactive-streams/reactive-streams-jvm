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

package org.reactivestreams.tck.flow;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.reactivestreams.tck.flow.support.SyncTriggeredDemandFlowSubscriber;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncTriggeredDemandSubscriberTest extends FlowSubscriberBlackboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncTriggeredDemandSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override
  public void triggerFlowRequest(Flow.Subscriber<? super Integer> subscriber) {
    ((SyncTriggeredDemandFlowSubscriber<? super Integer>) subscriber).triggerDemand(1);
  }

  @Override public Flow.Subscriber<Integer> createFlowSubscriber() {
    return new SyncTriggeredDemandFlowSubscriber<Integer>() {
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
