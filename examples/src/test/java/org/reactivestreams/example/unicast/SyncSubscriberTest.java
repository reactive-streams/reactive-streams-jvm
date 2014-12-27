package org.reactivestreams.example.unicast;

import java.util.Collections;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncSubscriberTest extends SubscriberBlackboxVerification<Integer> {

  final static long DefaultTimeoutMillis = 100;

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncSubscriberTest() {
    super(new TestEnvironment(DefaultTimeoutMillis));
  }

  @Override public Subscriber<Integer> createSubscriber() {
    return new SyncSubscriber<Integer>() {
      private long acc;
      @Override protected boolean foreach(final Integer element) {
        acc += element;
        return true;
      }

      @Override public void onComplete() {
        System.out.println("Accumulated: " + acc);
      }
    };
  }
  @SuppressWarnings("unchecked")
  @Override public Publisher<Integer> createHelperPublisher(long elements) {
    if (elements > Integer.MAX_VALUE) return new InfiniteIncrementNumberPublisher(e);
    else return new NumberIterablePublisher(0, (int)elements, e);
  }
}
