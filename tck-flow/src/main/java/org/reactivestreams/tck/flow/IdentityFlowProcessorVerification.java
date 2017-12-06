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

  protected abstract Flow.Publisher<T> createFailedFlowPublisher();

  protected abstract Flow.Processor<T,T> createIdentityFlowProcessor(int bufferSize);

  @Override
  public final Processor<T, T> createIdentityProcessor(int bufferSize) {
    return FlowAdapters.toProcessor(createIdentityFlowProcessor(bufferSize));
  }

  @Override
  public final Publisher<T> createFailedPublisher() {
    return FlowAdapters.toPublisher(createFailedFlowPublisher());
  }

}
