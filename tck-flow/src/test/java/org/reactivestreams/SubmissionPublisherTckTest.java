/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams;

import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.SubmissionPublisher;

@Test
public class SubmissionPublisherTckTest extends PublisherVerification<Integer> {

    public SubmissionPublisherTckTest() {
        super(new TestEnvironment(300));
    }

    @Override
    public Publisher<Integer> createPublisher(final long elements) {
        final SubmissionPublisher<Integer> sp = new SubmissionPublisher<Integer>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!sp.hasSubscribers()) {
                    Thread.yield();
                }
                for (int i = 0; i < elements; i++) {
                    sp.submit(i);
                }
                sp.close();
            }
        }).start();
        return FlowAdapters.toPublisher(sp);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        final SubmissionPublisher<Integer> sp = new SubmissionPublisher<Integer>();
        sp.closeExceptionally(new IOException());
        return FlowAdapters.toPublisher(sp);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 100;
    }

}