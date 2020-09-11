/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.example.unicast;

import org.reactivestreams.Publisher;
import org.reactivestreams.example.unicast.RangePublisher;
import org.reactivestreams.tck.*;

public class RangePublisherTest extends PublisherVerification<Integer> {
    public RangePublisherTest() {
        super(new TestEnvironment(50, 50));
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return new RangePublisher(1, (int)elements);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }
}
