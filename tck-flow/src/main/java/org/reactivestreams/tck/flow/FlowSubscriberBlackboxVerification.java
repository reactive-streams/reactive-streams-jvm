/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.SubscriberBlackboxVerificationRules;

import java.util.concurrent.Flow;

/**
 * Provides tests for verifying {@link java.util.concurrent.Flow.Subscriber} and {@link java.util.concurrent.Flow.Subscription}
 * specification rules, without any modifications to the tested implementation (also known as "Black Box" testing).
 *
 * This verification is NOT able to check many of the rules of the spec, and if you want more
 * verification of your implementation you'll have to implement {@code org.reactivestreams.tck.SubscriberWhiteboxVerification}
 * instead.
 *
 * @see java.util.concurrent.Flow.Subscriber
 * @see java.util.concurrent.Flow.Subscription
 */
public abstract class FlowSubscriberBlackboxVerification<T> extends SubscriberBlackboxVerification<T>
  implements SubscriberBlackboxVerificationRules {

  protected FlowSubscriberBlackboxVerification(TestEnvironment env) {
    super(env);
  }

  @Override
  public final void triggerRequest(Subscriber<? super T> subscriber) {
    triggerFlowRequest(FlowAdapters.toFlowSubscriber(subscriber));
  }
  /**
   * Override this method if the {@link java.util.concurrent.Flow.Subscriber} implementation you are verifying
   * needs an external signal before it signals demand to its Publisher.
   *
   * By default this method does nothing.
   */
  public void triggerFlowRequest(Flow.Subscriber<? super T> subscriber) {
    // this method is intentionally left blank
  }

  @Override
  public final Subscriber<T> createSubscriber() {
    return FlowAdapters.<T>toSubscriber(createFlowSubscriber());
  }
  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link Flow.Subscriber} instance to be subjected to the testing logic.
   */
  abstract public Flow.Subscriber<T> createFlowSubscriber();

}
