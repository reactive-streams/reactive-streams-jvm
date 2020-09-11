/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow;

import org.reactivestreams.*;
import org.reactivestreams.tck.*;
import org.testng.annotations.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Test
public class LockstepFlowProcessorTest extends IdentityFlowProcessorVerification<Integer> {

    public LockstepFlowProcessorTest() {
        super(new TestEnvironment());
    }
    @Override
    public Flow.Processor<Integer, Integer> createIdentityFlowProcessor(int bufferSize) {
        return new LockstepProcessor<Integer>();
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        LockstepProcessor<Integer> proc = new LockstepProcessor<Integer>();
        proc.onError(new Exception());
        return proc;
    }

    @Override
    public ExecutorService publisherExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

    @Override
    public long maxSupportedSubscribers() {
        return 2;
    }

    @Override
    public boolean doesCoordinatedEmission() {
        return true;
    }

    static final class LockstepProcessor<T> implements Flow.Processor<T, T> {

        final AtomicReference<LockstepSubscription<T>[]> subscribers =
                new AtomicReference<LockstepSubscription<T>[]>(EMPTY);

        static final LockstepSubscription[] EMPTY = new LockstepSubscription[0];
        static final LockstepSubscription[] TERMINATED = new LockstepSubscription[0];

        volatile boolean done;
        Throwable error;

        final AtomicReference<Flow.Subscription> upstream =
                new AtomicReference<Flow.Subscription>();

        final AtomicReferenceArray<T> queue =
                new AtomicReferenceArray<T>(BUFFER_MASK + 1);

        final AtomicLong producerIndex = new AtomicLong();

        final AtomicLong consumerIndex = new AtomicLong();

        final AtomicInteger wip = new AtomicInteger();

        static final int BUFFER_MASK = 127;

        int consumed;

        @Override
        public void subscribe(Flow.Subscriber<? super T> s) {
            LockstepSubscription<T> subscription = new LockstepSubscription<T>(s, this);
            s.onSubscribe(subscription);
            if (add(subscription)) {
                if (subscription.isCancelled()) {
                    remove(subscription);
                } else {
                    drain();
                }
            } else {
                Throwable ex = error;
                if (ex != null) {
                    s.onError(ex);
                } else {
                    s.onComplete();
                }
            }
        }

        boolean add(LockstepSubscription<T> sub) {
            for (;;) {
                LockstepSubscription<T>[] a = subscribers.get();
                if (a == TERMINATED) {
                    return false;
                }
                int n = a.length;
                LockstepSubscription<T>[] b = new LockstepSubscription[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = sub;
                if (subscribers.compareAndSet(a, b)) {
                    return true;
                }
            }
        }

        void remove(LockstepSubscription<T> sub) {
            for (;;) {
                LockstepSubscription<T>[] a = subscribers.get();
                int n = a.length;

                if (n == 0) {
                    break;
                }

                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == sub) {
                        j = i;
                        break;
                    }
                }

                if (j < 0) {
                    break;
                }
                LockstepSubscription<T>[] b;
                if (n == 1) {
                    b = TERMINATED;
                } else {
                    b = new LockstepSubscription[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (subscribers.compareAndSet(a, b)) {
                    if (b == TERMINATED) {
                        Flow.Subscription s = upstream.getAndSet(CancelledSubscription.INSTANCE);
                        if (s != null) {
                            s.cancel();
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                s.request(BUFFER_MASK + 1);
            } else {
                s.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            if (t == null) {
                throw new NullPointerException("t == null");
            }
            long pi = producerIndex.get();
            queue.lazySet((int)pi & BUFFER_MASK, t);
            producerIndex.lazySet(pi + 1);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (t == null) {
                throw new NullPointerException("t == null");
            }
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int limit = (BUFFER_MASK + 1) - ((BUFFER_MASK + 1) >> 2);
            int missed = 1;
            for (;;) {

                for (;;) {
                    LockstepSubscription<T>[] subscribers = this.subscribers.get();
                    int n = subscribers.length;

                    long ci = consumerIndex.get();

                    boolean d = done;
                    boolean empty = producerIndex.get() == ci;

                    if (d) {
                        Throwable ex = error;
                        if (ex != null) {
                            for (LockstepSubscription<T> sub : this.subscribers.getAndSet(TERMINATED)) {
                                sub.subscriber.onError(ex);
                            }
                            break;
                        } else if (empty) {
                            for (LockstepSubscription<T> sub : this.subscribers.getAndSet(TERMINATED)) {
                                sub.subscriber.onComplete();
                            }
                            break;
                        }
                    }

                    if (n != 0 && !empty) {
                        long ready = Long.MAX_VALUE;
                        int c = 0;
                        for (LockstepSubscription<T> sub : subscribers) {
                            long req = sub.get();
                            if (req != Long.MIN_VALUE) {
                                ready = Math.min(ready, req - sub.emitted);
                                c++;
                            }
                        }

                        if (ready != 0 && c != 0) {
                            int offset = (int) ci & BUFFER_MASK;
                            T value = queue.get(offset);
                            queue.lazySet(offset, null);
                            consumerIndex.lazySet(ci + 1);

                            for (LockstepSubscription<T> sub : subscribers) {
                                sub.subscriber.onNext(value);
                                sub.emitted++;
                            }

                            if (++consumed == limit) {
                                consumed = 0;
                                upstream.get().request(limit);
                            }
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class LockstepSubscription<T> extends AtomicLong
        implements Flow.Subscription {

            final Flow.Subscriber<? super T> subscriber;

            final LockstepProcessor<T> parent;

            long emitted;

            LockstepSubscription(Flow.Subscriber<? super T> subscriber, LockstepProcessor<T> parent) {
                this.subscriber = subscriber;
                this.parent = parent;
            }

            @Override
            public void request(long n) {
                if (n <= 0L) {
                    cancel();
                    subscriber.onError(new IllegalArgumentException("§3.9 violated: positive request amount required"));
                    return;
                }
                for (;;) {
                    long current = get();
                    if (current == Long.MIN_VALUE || current == Long.MAX_VALUE) {
                        break;
                    }

                    long updated = current + n;
                    if (updated < 0L) {
                        updated = Long.MAX_VALUE;
                    }
                    if (compareAndSet(current, updated)) {
                        parent.drain();
                        break;
                    }
                }
            }

            @Override
            public void cancel() {
                if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                    parent.remove(this);
                    parent.drain();
                }
            }

            boolean isCancelled() {
                return get() == Long.MIN_VALUE;
            }
        }
    }

    enum CancelledSubscription implements Flow.Subscription {

        INSTANCE;

        @Override
        public void request(long n) {
            // Subscription already cancelled
        }

        @Override
        public void cancel() {
            // Subscription already cancelled
        }
    }
}
