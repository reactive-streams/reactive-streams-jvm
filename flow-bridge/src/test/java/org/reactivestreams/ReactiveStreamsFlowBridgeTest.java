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

package org.reactivestreams;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class ReactiveStreamsFlowBridgeTest {
    @Test
    public void reactiveToFlowNormal() {
        MulticastPublisher<Integer> p = new MulticastPublisher<Integer>(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, Flow.defaultBufferSize());

        TestEitherConsumer<Integer> tc = new TestEitherConsumer<Integer>();

        ReactiveStreamsFlowBridge.toFlow(p).subscribe(tc);

        p.offer(1);
        p.offer(2);
        p.offer(3);
        p.offer(4);
        p.offer(5);
        p.complete();

        tc.assertRange(1, 5);
    }

    @Test
    public void reactiveToFlowError() {
        MulticastPublisher<Integer> p = new MulticastPublisher<Integer>(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, Flow.defaultBufferSize());

        TestEitherConsumer<Integer> tc = new TestEitherConsumer<Integer>();

        ReactiveStreamsFlowBridge.toFlow(p).subscribe(tc);

        p.offer(1);
        p.offer(2);
        p.offer(3);
        p.offer(4);
        p.offer(5);
        p.completeExceptionally(new IOException());

        tc.assertFailure(IOException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void flowToReactiveNormal() {
        SubmissionPublisher<Integer> p = new SubmissionPublisher<Integer>(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, Flow.defaultBufferSize());

        TestEitherConsumer<Integer> tc = new TestEitherConsumer<Integer>();

        ReactiveStreamsFlowBridge.toReactiveStreams(p).subscribe(tc);

        p.submit(1);
        p.submit(2);
        p.submit(3);
        p.submit(4);
        p.submit(5);
        p.close();

        tc.assertRange(1, 5);
    }

    @Test
    public void flowToReactiveError() {
        SubmissionPublisher<Integer> p = new SubmissionPublisher<Integer>(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, Flow.defaultBufferSize());

        TestEitherConsumer<Integer> tc = new TestEitherConsumer<Integer>();

        ReactiveStreamsFlowBridge.toReactiveStreams(p).subscribe(tc);

        p.submit(1);
        p.submit(2);
        p.submit(3);
        p.submit(4);
        p.submit(5);
        p.closeExceptionally(new IOException());

        tc.assertFailure(IOException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void reactiveStreamsToFlowSubscriber() {
        TestEitherConsumer<Integer> tc = new TestEitherConsumer<Integer>();

        Flow.Subscriber<Integer> fs = ReactiveStreamsFlowBridge.toFlow(tc);

        final Object[] state = { null, null };

        fs.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                state[0] = n;
            }

            @Override
            public void cancel() {
                state[1] = true;
            }
        });

        Assert.assertEquals(state[0], Long.MAX_VALUE);

        fs.onNext(1);
        fs.onNext(2);
        fs.onNext(3);
        fs.onComplete();

        tc.assertResult(1, 2, 3);

        Assert.assertNull(state[1]);
    }

    @Test
    public void flowToReactiveStreamsSubscriber() {
        TestEitherConsumer<Integer> tc = new TestEitherConsumer<Integer>();

        org.reactivestreams.Subscriber<Integer> fs = ReactiveStreamsFlowBridge.toReactiveStreams(tc);

        final Object[] state = { null, null };

        fs.onSubscribe(new org.reactivestreams.Subscription() {
            @Override
            public void request(long n) {
                state[0] = n;
            }

            @Override
            public void cancel() {
                state[1] = true;
            }
        });

        Assert.assertEquals(state[0], Long.MAX_VALUE);

        fs.onNext(1);
        fs.onNext(2);
        fs.onNext(3);
        fs.onComplete();

        tc.assertResult(1, 2, 3);

        Assert.assertNull(state[1]);
    }
}
