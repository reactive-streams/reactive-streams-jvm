package org.reactivestreams.example.unicast;

import java.util.Collections;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

@Test // Must be here for TestNG to find and run this, do not remove
public class AsyncSubscriberTest extends SubscriberBlackboxVerification<Integer> {
  final static long DefaultTimeoutMillis = 100;

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public AsyncSubscriberTest() {
    super(new TestEnvironment(DefaultTimeoutMillis));
  }

  @Override public Subscriber<Integer> createSubscriber() {
    return new AsyncSubscriber<Integer>(e) {
      private long acc;
      @Override protected boolean whenNext(final Integer element) {
        return true;
      }
    };
  }
  @SuppressWarnings("unchecked")
  @Override public Publisher<Integer> createHelperPublisher(long elements) {
    if (elements > Integer.MAX_VALUE) return new InfiniteIncrementNumberPublisher(e);
    else return new NumberIterablePublisher(0, (int)elements, e);
  }

  @Test public void testAccumulation() throws InterruptedException {

    final AtomicLong i = new AtomicLong(Long.MIN_VALUE);
    final CountDownLatch latch = new CountDownLatch(1);
    final Subscriber<Integer> sub =  new AsyncSubscriber<Integer>(e) {
      private long acc;
      @Override protected boolean whenNext(final Integer element) {
        acc += element;
        return true;
      }

      @Override protected void whenComplete() {
        i.set(acc);
        latch.countDown();
      }
    };

    new NumberIterablePublisher<Integer>(0, 10, e).subscribe(sub);
    latch.await(DefaultTimeoutMillis * 10, TimeUnit.MILLISECONDS);
    assertEquals(i.get(), 45);
  }
}
