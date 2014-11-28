package org.reactivestreams.tck;

import org.testng.annotations.Test;

public class TestEnvironmentTest {

  final TestEnvironment env = new TestEnvironment(100);

  @Test
  public void assertAsyncErrorWithMessage_shouldNotThrowWhenExpectedException() throws Throwable {
    env.flop(new ExampleException("3.17"), "boom");

    env.assertAsyncErrorWithMessage(ExampleException.class, "3.17");
  }

  @Test(expectedExceptions = AssertionError.class)
  public void assertAsyncErrorWithMessage_shouldThrowWhenExceptionHasMissingText() throws Throwable {
    env.flop(new ExampleException("3.17"), "boom");

    env.assertAsyncErrorWithMessage(ExampleException.class, "wat");
  }

  @Test
  public void expectThrowingOfWithMessage_shouldNotThrowWhenExpectedExceptionThrown() throws Throwable {
    env.expectThrowingOfWithMessage(ExampleException.class, "3.17", new Runnable() {
      @Override public void run() {
        throw new ExampleException("3.17");
      }
    });
  }

  @Test(expectedExceptions = AssertionError.class)
  public void expectThrowingOfWithMessage_shouldThrowWhenExpectedExceptionHasMissingText() throws Throwable {
    env.expectThrowingOfWithMessage(ExampleException.class, "3.17", new Runnable() {
      @Override public void run() {
        throw new ExampleException("3");
      }
    });
  }

  @Test(expectedExceptions = AssertionError.class)
  public void expectThrowingOfWithMessage_shouldThrowWhenNoExceptionWasThrown() throws Throwable {
    env.expectThrowingOfWithMessage(ExampleException.class, "3.17", new Runnable() {
      @Override public void run() {
      }
    });
  }
}

final class ExampleException extends RuntimeException {
  public ExampleException(String message) {
    super(message);
  }
}