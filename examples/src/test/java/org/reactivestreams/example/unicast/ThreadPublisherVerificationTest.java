package org.reactivestreams.example.unicast;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

@Test // Must be here for TestNG to find and run this, do not remove
public class ThreadPublisherVerificationTest extends org.reactivestreams.tck.PublisherVerification {
    static final long defaultTimeout = 400;

    public ThreadPublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        ThreadPublisher publisher = new ThreadPublisher(elements);
        publisher.start();
        return publisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

}
