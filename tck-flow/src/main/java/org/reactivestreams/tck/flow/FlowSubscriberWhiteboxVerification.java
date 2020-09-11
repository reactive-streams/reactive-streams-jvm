/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/
package org.reactivestreams.tck.flow;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.SubscriberWhiteboxVerificationRules;

import java.util.concurrent.Flow;

/**
 * Provides whitebox style tests for verifying {@link java.util.concurrent.Flow.Subscriber}
 * and {@link java.util.concurrent.Flow.Subscription} specification rules.
 *
 * @see java.util.concurrent.Flow.Subscriber
 * @see java.util.concurrent.Flow.Subscription
 */
public abstract class FlowSubscriberWhiteboxVerification<T> extends SubscriberWhiteboxVerification<T>
  implements SubscriberWhiteboxVerificationRules {

  protected FlowSubscriberWhiteboxVerification(TestEnvironment env) {
    super(env);
  }

  @Override
  final public Subscriber<T> createSubscriber(WhiteboxSubscriberProbe<T> probe) {
    return FlowAdapters.toSubscriber(createFlowSubscriber(probe));
  }
  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link org.reactivestreams.Subscriber} instance to be subjected to the testing logic.
   *
   * In order to be meaningfully testable your Subscriber must inform the given
   * `WhiteboxSubscriberProbe` of the respective events having been received.
   */
  protected abstract Flow.Subscriber<T> createFlowSubscriber(WhiteboxSubscriberProbe<T> probe);
}
