package org.reactivestreams.example.unicast;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

@Test
public class InfiniteIncrementNumberPublisherTest extends PublisherVerification<Integer> {

  final static long DefaultTimeoutMillis = 100;
  final static long PublisherReferenceGCTimeoutMillis = 300;

  public InfiniteIncrementNumberPublisherTest() {
    super(new TestEnvironment(DefaultTimeoutMillis), PublisherReferenceGCTimeoutMillis);
  }

  @Override public Publisher<Integer> createPublisher(long elements) {
    return new InfiniteIncrementNumberPublisher();
  }

  @Override public Publisher<Integer> createErrorStatePublisher() {
    return null;
  }

  @Override public long maxElementsFromPublisher() {
    return super.publisherUnableToSignalOnComplete();
  }
}
