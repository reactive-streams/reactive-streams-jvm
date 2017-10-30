/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

package org.reactivestreams.tck.flow;

import org.reactivestreams.ReactiveStreamsFlowBridge;
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
    triggerFlowRequest(ReactiveStreamsFlowBridge.toFlowSubscriber(subscriber));
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
    return ReactiveStreamsFlowBridge.<T>toSubscriber(createFlowSubscriber());
  }
  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link Flow.Subscriber} instance to be subjected to the testing logic.
   */
  abstract public Flow.Subscriber<T> createFlowSubscriber();

}
