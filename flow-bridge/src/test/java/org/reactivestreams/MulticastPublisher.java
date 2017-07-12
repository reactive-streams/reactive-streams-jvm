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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.*;

final class MulticastPublisher<T> implements Publisher<T>, AutoCloseable {

    final Executor executor;
    final int bufferSize;

    final AtomicBoolean done = new AtomicBoolean();
    Throwable error;

    static final InnerSubscription[] EMPTY = new InnerSubscription[0];
    static final InnerSubscription[] TERMINATED = new InnerSubscription[0];


    final AtomicReference<InnerSubscription<T>[]> subscribers = new AtomicReference<InnerSubscription<T>[]>();

    public MulticastPublisher() {
        this(ForkJoinPool.commonPool(), Flow.defaultBufferSize());
    }

    @SuppressWarnings("unchecked")
    public MulticastPublisher(Executor executor, int bufferSize) {
        if ((bufferSize & (bufferSize - 1)) != 0) {
            throw new IllegalArgumentException("Please provide a power-of-two buffer size");
        }
        this.executor = executor;
        this.bufferSize = bufferSize;
        subscribers.setRelease(EMPTY);
    }

    public boolean offer(T item) {
        Objects.requireNonNull(item, "item is null");

        InnerSubscription<T>[] a = subscribers.get();
        synchronized (this) {
            for (InnerSubscription<T> inner : a) {
                if (inner.isFull()) {
                    return false;
                }
            }
            for (InnerSubscription<T> inner : a) {
                inner.offer(item);
                inner.drain(executor);
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public void complete() {
        if (done.compareAndSet(false, true)) {
            for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                inner.done = true;
                inner.drain(executor);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void completeExceptionally(Throwable error) {
        if (done.compareAndSet(false, true)) {
            this.error = error;
            for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                inner.error = error;
                inner.done = true;
                inner.drain(executor);
            }
        }
    }

    @Override
    public void close() {
        complete();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        InnerSubscription<T> inner = new InnerSubscription<T>(subscriber, bufferSize, this);
        if (!add(inner)) {
            Throwable ex = error;
            if (ex != null) {
                inner.error = ex;
            }
            inner.done = true;
        }
        inner.drain(executor);
    }

    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    boolean add(InnerSubscription<T> inner) {

        for (;;) {
            InnerSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            InnerSubscription<T>[] b = new InnerSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(InnerSubscription<T> inner) {
        for (;;) {
            InnerSubscription<T>[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                break;
            }

            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                break;
            }
            InnerSubscription<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new InnerSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                break;
            }
        }
    }

    static final class InnerSubscription<T> implements Subscription, Runnable {

        final Subscriber<? super T> actual;
        final MulticastPublisher<T> parent;
        final AtomicReferenceArray<T> queue;
        final int mask;

        volatile boolean badRequest;
        final AtomicBoolean cancelled = new AtomicBoolean();

        volatile boolean done;
        Throwable error;

        boolean subscribed;
        long emitted;

        final AtomicLong requested = new AtomicLong();

        final AtomicInteger wip = new AtomicInteger();

        final AtomicLong producerIndex = new AtomicLong();

        final AtomicLong consumerIndex = new AtomicLong();

        InnerSubscription(Subscriber<? super T> actual, int bufferSize, MulticastPublisher<T> parent) {
            this.actual = actual;
            this.queue = new AtomicReferenceArray<T>(bufferSize);
            this.parent = parent;
            this.mask = bufferSize - 1;
        }

        void offer(T item) {
            AtomicReferenceArray<T> q = queue;
            int m = mask;
            long pi = producerIndex.get();
            int offset = (int)(pi) & m;

            q.setRelease(offset, item);
            producerIndex.setRelease(pi + 1);
        }

        T poll() {
            AtomicReferenceArray<T> q = queue;
            int m = mask;
            long ci = consumerIndex.get();

            int offset = (int)(ci) & m;
            T o = q.getAcquire(offset);
            if (o != null) {
                q.setRelease(offset, null);
                consumerIndex.setRelease(ci + 1);
            }
            return o;
        }

        boolean isFull() {
            return 1 + mask + consumerIndex.get() == producerIndex.get();
        }

        void drain(Executor executor) {
            if (wip.getAndAdd(1) == 0) {
                executor.execute(this);
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                badRequest = true;
                done = true;
            } else {
                for (;;) {
                    long r = requested.get();
                    long u = r + n;
                    if (u < 0) {
                        u = Long.MAX_VALUE;
                    }
                    if (requested.compareAndSet(r, u)) {
                        break;
                    }
                }
            }
            drain(parent.executor);
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                parent.remove(this);
            }
        }

        void clear() {
            error = null;
            while (poll() != null) ;
        }

        @Override
        public void run() {
            int missed = 1;
            Subscriber<? super T> a = actual;

            outer:
            for (;;) {

                if (subscribed) {
                    if (cancelled.get()) {
                        clear();
                    } else {
                        long r = requested.get();
                        long e = emitted;

                        while (e != r) {
                            if (cancelled.get()) {
                                continue outer;
                            }

                            boolean d = done;

                            if (d) {
                                Throwable ex = error;
                                if (ex != null) {
                                    cancelled.setRelease(true);
                                    a.onError(ex);
                                    continue outer;
                                }
                                if (badRequest) {
                                    cancelled.setRelease(true);
                                    parent.remove(this);
                                    a.onError(new IllegalArgumentException("§3.9 violated: request was not positive"));
                                    continue outer;
                                }
                            }

                            T v = poll();
                            boolean empty = v == null;

                            if (d && empty) {
                                cancelled.setRelease(true);
                                a.onComplete();
                                break;
                            }

                            if (empty) {
                                break;
                            }

                            a.onNext(v);

                            e++;
                        }

                        if (e == r) {
                            if (cancelled.get()) {
                                continue outer;
                            }
                            if (done) {
                                Throwable ex = error;
                                if (ex != null) {
                                    cancelled.setRelease(true);
                                    a.onError(ex);
                                } else
                                if (badRequest) {
                                    cancelled.setRelease(true);
                                    a.onError(new IllegalArgumentException("§3.9 violated: request was not positive"));
                                } else
                                if (producerIndex == consumerIndex) {
                                    cancelled.setRelease(true);
                                    a.onComplete();
                                }
                            }
                        }

                        emitted = e;
                    }
                } else {
                    subscribed = true;
                    a.onSubscribe(this);
                }

                int w = wip.get();
                if (missed == w) {
                    w = wip.getAndAdd(-missed);
                    if (missed == w) {
                        break;
                    }
                }
                missed = w;
            }
        }
    }
}