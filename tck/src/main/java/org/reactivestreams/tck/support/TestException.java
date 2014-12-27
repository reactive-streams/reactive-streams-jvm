package org.reactivestreams.tck.support;

/**
 * Exception used by the TCK to signal failures.
 * May be thrown or signalled through {@link org.reactivestreams.Subscriber#onError(Throwable)}.
 */
public final class TestException extends RuntimeException {
  public TestException() {
    super("Test Exception: Boom!");
  }
}
