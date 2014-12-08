package org.reactivestreams.tck.support;

public final class SubscriberBufferOverflowException extends RuntimeException {
  public SubscriberBufferOverflowException() {
  }

  public SubscriberBufferOverflowException(String message) {
    super(message);
  }

  public SubscriberBufferOverflowException(String message, Throwable cause) {
    super(message, cause);
  }

  public SubscriberBufferOverflowException(Throwable cause) {
    super(cause);
  }
}
