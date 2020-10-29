/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck;

import org.reactivestreams.example.unicast.AsyncIterablePublisher;
import org.reactivestreams.Publisher;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test
public class EmptyLazyPublisherTest extends PublisherVerification<Integer> {

  private ExecutorService ex;

  public EmptyLazyPublisherTest() {
    super(new TestEnvironment());
  }

  @BeforeClass
  void before() { ex = Executors.newFixedThreadPool(4); }

  @AfterClass
  void after() { if (ex != null) ex.shutdown(); }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    return new AsyncIterablePublisher<Integer>(Collections.<Integer>emptyList(), ex);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return null;
  }

  @Override
  public long maxElementsFromPublisher() {
    return 0;
  }
}
