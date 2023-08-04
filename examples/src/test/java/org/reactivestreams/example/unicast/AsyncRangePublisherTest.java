/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/
package org.reactivestreams.example.unicast;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Test // Must be here for TestNG to find and run this, do not remove
public class AsyncRangePublisherTest extends PublisherVerification<Integer> {
    private static final int TERMINAL_DELAY_MS = 20;
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_POLL_INTERVAL_MS = TERMINAL_DELAY_MS / 2;
    private ExecutorService e;
    @BeforeClass
    void before() { e = Executors.newCachedThreadPool(); }
    @AfterClass
    void after() { if (e != null) e.shutdown(); }

    public AsyncRangePublisherTest() {
        super(new TestEnvironment(DEFAULT_TIMEOUT_MS, 50, DEFAULT_POLL_INTERVAL_MS));
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return new AsyncPublisher<Integer>(new RangePublisher(1, (int)elements), e);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }

    private static final class AsyncPublisher<T> implements Publisher<T> {
        private final Publisher<T> original;
        private final Executor executor;

        private AsyncPublisher(Publisher<T> original, Executor executor) {
            this.original = requireNonNull(original);
            this.executor = requireNonNull(executor);
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            AsyncSubscriber.wrapAndSubscribe(original, requireNonNull(s), executor);
        }

        private static final class AsyncSubscriber<T> implements Subscriber<T> {
            private final BlockingQueue<Object> signalQueue = new LinkedBlockingQueue<Object>();

            static <T> void wrapAndSubscribe(final Publisher<T> publisher,
                    final Subscriber<? super T> targetSubscriber, final Executor executor) {
                final AsyncSubscriber<T> asyncSubscriber = new AsyncSubscriber<T>();
                try {
                    executor.execute(new Runnable() {
                        private Subscription subscription;
                        private boolean terminated;
                        @Override
                        public void run() {
                            try {
                                for (; ; ) {
                                    final Object signal = asyncSubscriber.signalQueue.take();
                                    if (signal instanceof Cancelled) {
                                        return;
                                    } else if (signal instanceof TerminalSignal) {
                                        // sleep intentional to verify TestEnvironment.expectError behavior.
                                        Thread.sleep(TERMINAL_DELAY_MS);

                                        TerminalSignal terminalSignal = (TerminalSignal) signal;
                                        terminated = true;
                                        if (terminalSignal.cause == null) {
                                            targetSubscriber.onComplete();
                                        } else {
                                            targetSubscriber.onError(terminalSignal.cause);
                                        }
                                        return;
                                    } else if (signal instanceof OnSubscribeSignal) {
                                        // We distribute the subscription downstream and may also call cancel on this
                                        // thread if an exception is thrown. Since there is no concurrency allowed, make
                                        // the subscription safe for concurrency.
                                        subscription = concurrentSafe(((OnSubscribeSignal) signal).subscription);
                                        targetSubscriber.onSubscribe(subscription);
                                    } else {
                                        @SuppressWarnings("unchecked") final T onNextSignal = ((OnNextSignal<T>) signal).onNext;
                                        targetSubscriber.onNext(onNextSignal);
                                    }
                                }
                            } catch (Throwable cause) {
                                if (!terminated) {
                                    try {
                                        if (subscription == null) {
                                            targetSubscriber.onSubscribe(noopSubscription());
                                        } else {
                                            subscription.cancel();
                                        }
                                    } finally {
                                        terminated = true;
                                        targetSubscriber.onError(new IllegalStateException("run loop interrupted", cause));
                                    }
                                }
                            }
                        }
                    });
                } catch (Throwable cause) {
                    try {
                        targetSubscriber.onSubscribe(noopSubscription());
                    } finally {
                        targetSubscriber.onError(new IllegalStateException("Executor rejected", cause));
                    }
                    // Publisher rejected the target subscriber and terminated it, don't continue to subscribe to avoid
                    // duplicate termination.
                    return;
                }
                publisher.subscribe(asyncSubscriber);
            }

            @Override
            public void onSubscribe(final Subscription s) {
                signalQueue.add(new OnSubscribeSignal(new Subscription() {
                    @Override
                    public void request(long n) {
                        s.request(n);
                    }

                    @Override
                    public void cancel() {
                        try {
                            s.cancel();
                        } finally {
                            signalQueue.add(new Cancelled());
                        }
                    }
                }));
            }

            @Override
            public void onNext(T t) {
                signalQueue.add(new OnNextSignal<T>(t));
            }

            @Override
            public void onError(Throwable t) {
                signalQueue.add(new TerminalSignal(requireNonNull(t)));
            }

            @Override
            public void onComplete() {
                signalQueue.add(new TerminalSignal(null));
            }
        }

        private static Subscription concurrentSafe(final Subscription subscription) {
            // TODO: make concurrent safe. TCK interacts from the subscription concurrently so lock isn't sufficient.
            return subscription;
        }

        private static Subscription noopSubscription() {
            return new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            };
        }

        private static final class TerminalSignal {
            private final Throwable cause;

            private TerminalSignal(Throwable cause) {
                this.cause = cause;
            }
        }

        private static final class OnSubscribeSignal {
            private final Subscription subscription;

            private OnSubscribeSignal(Subscription subscription) {
                this.subscription = subscription;
            }
        }

        private static final class OnNextSignal<T> {
            private final T onNext;

            private OnNextSignal(T onNext) {
                this.onNext = onNext;
            }
        }

        private static final class Cancelled {
        }

        private static <T> T requireNonNull(T o) {
            if (o == null) {
                throw new NullPointerException();
            }
            return o;
        }
    }
}
