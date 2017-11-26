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

package org.reactivestreams.tck;

import java.util.concurrent.*;

import org.reactivestreams.*;

public class ReplayProcessorFailFastAsProcessorTest extends IdentityProcessorVerification<Integer> {

    public ReplayProcessorFailFastAsProcessorTest() {
        super(new TestEnvironment());
    }

    @Override
    public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
        return new ReplayProcessor<Integer>(false);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }

    @Override
    public ExecutorService publisherExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

}
