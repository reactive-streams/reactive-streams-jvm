/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.example.unicast.AsyncIterablePublisher;
import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test
public class EmptyLazyFlowPublisherTest extends FlowPublisherVerification<Integer> {

  private ExecutorService ex;

  public EmptyLazyFlowPublisherTest() {
    super(new TestEnvironment());
  }

  @BeforeClass
  void before() { ex = Executors.newFixedThreadPool(4); }

  @AfterClass
  void after() { if (ex != null) ex.shutdown(); }

  @Override
  public Publisher<Integer> createFlowPublisher(long elements) {
    return FlowAdapters.toFlowPublisher(
      new AsyncIterablePublisher<Integer>(Collections.<Integer>emptyList(), ex)
    );
  }

  @Override
  public Publisher<Integer> createFailedFlowPublisher() {
    return null;
  }

  @Override
  public long maxElementsFromPublisher() {
    return 0;
  }
}
