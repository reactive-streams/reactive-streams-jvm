package org.reactivestreams.example.unicast;

import java.util.Collections;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Test // Must be here for TestNG to find and run this, do not remove
public class IterablePublisherTest extends PublisherVerification<Integer> {

  final static long DefaultTimeoutMillis = 100;
  final static long PublisherReferenceGCTimeoutMillis = 300;

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public IterablePublisherTest() {
    super(new TestEnvironment(DefaultTimeoutMillis), PublisherReferenceGCTimeoutMillis);
  }

  @SuppressWarnings("unchecked")
  @Override public Publisher<Integer> createPublisher(final long elements) {
    assert(elements <= maxElementsFromPublisher());
    return new NumberIterablePublisher(0, (int)elements, e);
  }

  @Override public Publisher<Integer> createErrorStatePublisher() {
    return null;
  }

  @Override public long maxElementsFromPublisher() {
    return Integer.MAX_VALUE;
  }
}
