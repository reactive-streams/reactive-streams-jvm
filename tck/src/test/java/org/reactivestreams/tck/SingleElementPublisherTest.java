/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.example.unicast.AsyncIterablePublisher;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test
public class SingleElementPublisherTest extends PublisherVerification<Integer> {

  private ExecutorService ex;

  public SingleElementPublisherTest() {
    super(new TestEnvironment());
  }

  @BeforeClass
  void before() { ex = Executors.newFixedThreadPool(4); }

  @AfterClass
  void after() { if (ex != null) ex.shutdown(); }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    return new AsyncIterablePublisher<Integer>(Collections.singleton(1), ex);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return null;
  }

  @Override
  public long maxElementsFromPublisher() {
    return 1;
  }
}
