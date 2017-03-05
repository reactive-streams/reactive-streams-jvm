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
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncSubscriberTest extends SubscriberBlackboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override public Subscriber<Integer> createSubscriber() {
    return new SyncSubscriber<Integer>() {
      private long acc;
      @Override protected boolean whenNext(final Integer element) {
        acc += element;
        return true;
      }

      @Override public void onComplete() {
        System.out.println("Accumulated: " + acc);
      }
    };
  }

  @Override public Integer createElement(int element) {
    return element;
  }
}
