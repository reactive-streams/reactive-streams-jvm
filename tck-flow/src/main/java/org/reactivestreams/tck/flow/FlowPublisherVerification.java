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

import org.reactivestreams.Publisher;
import org.reactivestreams.ReactiveStreamsFlowBridge;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.concurrent.Flow;

/**
 * Provides tests for verifying a Java 9+ {@link java.util.concurrent.Flow.Publisher} specification rules.
 *
 * @see java.util.concurrent.Flow.Publisher
 */
public abstract class FlowPublisherVerification<T> extends PublisherVerification<T> {

  public FlowPublisherVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis) {
    super(env, publisherReferenceGCTimeoutMillis);
  }

  public FlowPublisherVerification(TestEnvironment env) {
    super(env);
  }

  @Override
  final public Publisher<T> createPublisher(long elements) {
    final Flow.Publisher<T> flowPublisher = createFlowPublisher(elements);
    return ReactiveStreamsFlowBridge.toPublisher(flowPublisher);
  }
  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If `elements` is `Long.MAX_VALUE` the produced stream must be infinite.
   */
  public abstract Flow.Publisher<T> createFlowPublisher(long elements);

  @Override
  final public Publisher<T> createFailedPublisher() {
    final Flow.Publisher<T> failed = createFailedFlowPublisher();
    if (failed == null) return null; // because `null` means "SKIP" in createFailedPublisher
    else return ReactiveStreamsFlowBridge.toPublisher(failed);
  }
  /**
   * By implementing this method, additional TCK tests concerning a "failed" publishers will be run.
   *
   * The expected behaviour of the {@link Flow.Publisher} returned by this method is hand out a subscription,
   * followed by signalling {@code onError} on it, as specified by Rule 1.9.
   *
   * If you ignore these additional tests, return {@code null} from this method.
   */
  public abstract Flow.Publisher<T> createFailedFlowPublisher();
}
