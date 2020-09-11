/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow;

import org.reactivestreams.*;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.SubscriberWhiteboxVerificationRules;
import org.reactivestreams.tck.flow.support.PublisherVerificationRules;

import java.util.concurrent.Flow;

public abstract class IdentityFlowProcessorVerification<T> extends IdentityProcessorVerification<T>
  implements SubscriberWhiteboxVerificationRules, PublisherVerificationRules {

  public IdentityFlowProcessorVerification(TestEnvironment env) {
    super(env);
  }

  public IdentityFlowProcessorVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis) {
    super(env, publisherReferenceGCTimeoutMillis);
  }

  public IdentityFlowProcessorVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis, int processorBufferSize) {
    super(env, publisherReferenceGCTimeoutMillis, processorBufferSize);
  }

  /**
   * By implementing this method, additional TCK tests concerning a "failed" Flow publishers will be run.
   *
   * The expected behaviour of the {@link Flow.Publisher} returned by this method is hand out a subscription,
   * followed by signalling {@code onError} on it, as specified by Rule 1.9.
   *
   * If you want to ignore these additional tests, return {@code null} from this method.
   */
  protected abstract Flow.Publisher<T> createFailedFlowPublisher();

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a {@link Flow.Processor}, which simply forwards all stream elements from its upstream
   * to its downstream. It must be able to internally buffer the given number of elements.
   *
   * @param bufferSize number of elements the processor is required to be able to buffer.
   */
  protected abstract Flow.Processor<T,T> createIdentityFlowProcessor(int bufferSize);

  @Override
  public final Processor<T, T> createIdentityProcessor(int bufferSize) {
    return FlowAdapters.toProcessor(createIdentityFlowProcessor(bufferSize));
  }

  @Override
  public final Publisher<T> createFailedPublisher() {
    Flow.Publisher<T> failed = createFailedFlowPublisher();
    if (failed == null) return null; // because `null` means "SKIP" in createFailedPublisher
    else return FlowAdapters.toPublisher(failed);
  }

}
