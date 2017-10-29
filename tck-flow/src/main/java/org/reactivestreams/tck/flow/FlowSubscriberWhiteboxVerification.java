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
    return ReactiveStreamsFlowBridge.toSubscriber(createFlowSubscriber(probe));
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
