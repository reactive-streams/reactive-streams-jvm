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
