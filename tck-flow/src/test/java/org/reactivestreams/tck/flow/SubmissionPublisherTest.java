/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow;

import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.util.concurrent.*;

@Test
public class SubmissionPublisherTest extends FlowPublisherVerification<Integer> {

    public SubmissionPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(final long elements) {
        final SubmissionPublisher<Integer> sp = new SubmissionPublisher<Integer>();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (sp.getNumberOfSubscribers() == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }

                for (int i = 0; i < elements; i++) {
                    sp.submit(i);
                }

                sp.close();
            }
        });
        t.setDaemon(true);
        t.start();

        return sp;
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<Integer>();
        sp.closeExceptionally(new Exception());
        return sp;
    }
}
