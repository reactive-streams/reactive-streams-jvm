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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * AsyncStreamPublisher is an implementation of Reactive Streams `Publisher`
 * which executes asynchronously, using a provided `Executor` and produces elements
 * from a given `StreamSupplier` in a "unicast" configuration to its `Subscribers`.
 * <p>
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
class AsyncStreamPublisher<T> implements Publisher<T> {

    private final static int DEFAULT_BATCHSIZE = 1024;

    private final StreamSupplier<T> supplier; // This is our data source / generator
    private final Executor executor; // This is our thread pool, which will make sure that our Publisher runs asynchronously to its Subscribers
    private final int batchSize; // In general, if one uses an `Executor`, one should be nice nad not hog a thread for too long, this is the cap for that, in elements

    public AsyncStreamPublisher(final StreamSupplier<T> supplier, final Executor executor) {
        this(supplier, DEFAULT_BATCHSIZE, executor);
    }

    public AsyncStreamPublisher(final StreamSupplier<T> supplier, final int batchSize, final Executor executor) {
        if (supplier == null) {
            throw null;
        }
        if (executor == null) {
            throw null;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero!");
        }
        this.supplier = supplier;
        this.executor = executor;
        this.batchSize = batchSize;
    }

    @Override
    public void subscribe(final Subscriber<? super T> s) {
        // As per rule 1.11, we have decided to support multiple subscribers in a unicast configuration
        // for this `Publisher` implementation.
        // As per 2.13, this method must return normally (i.e. not throw)
        new SubscriptionImpl(s).init();
    }
    
    static interface StreamSupplier<T> {
        T get();
    } 

    // These represent the protocol of the `AsyncIterablePublishers` SubscriptionImpls
    static interface Signal {
    }

    enum Cancel implements Signal {Instance}

    enum Subscribe implements Signal {Instance}

    enum Send implements Signal {Instance}

    static final class Request implements Signal {
        final long n;

        Request(final long n) {
            this.n = n;
        }
    }

    // This is our implementation of the Reactive Streams `Subscription`,
    // which represents the association between a `Publisher` and a `Subscriber`.
    final class SubscriptionImpl implements Subscription, Runnable {
        final Subscriber<? super T> subscriber; // We need a reference to the `Subscriber` so we can talk to it
        private boolean cancelled = false; // This flag will track whether this `Subscription` is to be considered cancelled or not
        private long demand = 0; // Here we track the current demand, i.e. what has been requested but not yet delivered
        private T nextElementToBeSent = null; // we need to fetch an element by the subscription initialization to ensure the stream is not empty

        SubscriptionImpl(final Subscriber<? super T> subscriber) {
            // As per rule 1.09, we need to throw a `java.lang.NullPointerException` if the `Subscriber` is `null`
            if (subscriber == null) {
                throw null;
            }
            this.subscriber = subscriber;
        }

        // This `ConcurrentLinkedQueue` will track signals that are sent to this `Subscription`, like `request` and `cancel`
        private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<Signal>();

        // We are using this `AtomicBoolean` to make sure that this `Subscription` doesn't run concurrently with itself,
        // which would violate rule 1.3 among others (no concurrent notifications).
        private final AtomicBoolean on = new AtomicBoolean(false);

        // This method will register inbound demand from our `Subscriber` and validate it against rule 3.9 and rule 3.17
        private void doRequest(final long n) {
            if (n < 1) {
                terminateDueTo(new IllegalArgumentException(subscriber + " violated the Reactive Streams rule 3.9 by requesting a non-positive number of elements."));
            } else if (demand + n < 1) {
                // As governed by rule 3.17, when demand overflows `Long.MAX_VALUE` we treat the signalled demand as "effectively unbounded"
                demand = Long.MAX_VALUE;  // Here we protect from the overflow and treat it as "effectively unbounded"
                doSend(); // Then we proceed with sending data downstream
            } else {
                demand += n; // Here we record the downstream demand
                doSend(); // Then we can proceed with sending data downstream
            }
        }

        // This handles cancellation requests, and is idempotent, thread-safe and not synchronously performing heavy computations as specified in rule 3.5
        private void doCancel() {
            cancelled = true;
        }

        // Instead of executing `subscriber.onSubscribe` synchronously from within `Publisher.subscribe`
        // we execute it asynchronously, this is to avoid executing the user code (`supplier`) on the calling thread.
        // It also makes it easier to follow rule 1.9
        private void doSubscribe() {
            // Deal with setting up the subscription with the subscriber
            try {
                subscriber.onSubscribe(this);
            } catch (final Throwable t) { // Due diligence to obey 2.13
                terminateDueTo(new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onSubscribe.", t));
            }
            if (!cancelled) {
                // Deal with already complete iterators promptly
                boolean hasElements = false;
                try {
                    // Try to fetch an element from a stream to ensure the stream is not empty,
                    // this will be sent by the first calling of doSend
                    nextElementToBeSent = supplier.get();
                    hasElements = nextElementToBeSent != null;
                } catch (final Throwable t) {
                    terminateDueTo(t); // If hasNext throws, there's something wrong and we need to signal onError as per 1.2, 1.4
                    return;
                }
    
                // If we don't have anything to deliver, we're already done, so lets do the right thing and
                // not wait for demand to deliver `onComplete` as per rule 1.2 and 1.3
                if (!hasElements) {
                    try {
                        doCancel(); // Rule 1.6 says we need to consider the `Subscription` cancelled when `onComplete` is signalled
                        subscriber.onComplete();
                    } catch (final Throwable t) { // As per rule 2.13, `onComplete` is not allowed to throw exceptions, so we do what we can, and log this.
                        (new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onComplete.", t)).printStackTrace(System.err);
                    }
                }
            }
        }

        // This is our behavior for producing elements downstream
        private void doSend() {
            try {
                // In order to play nice with the `Executor` we will only send at-most `batchSize` before
                // rescheduing ourselves and relinquishing the current thread.
                int leftInBatch = batchSize;
                do {
                    T next;
                    boolean hasNext;
                    try {
                        next = supplier.get(); // We have already checked `hasNext` when subscribing, so we can fall back to testing -after- `next` is called.
                        hasNext = next != null; // Need to keep track of End-of-Stream
                    } catch (final Throwable t) {
                        terminateDueTo(t); // If `next` or `hasNext` throws (they can, since it is user-provided), we need to treat the stream as errored as per rule 1.4
                        return;
                    } finally {
                        subscriber.onNext(nextElementToBeSent); // Then we signal the next element downstream to the `Subscriber`
                    }
                    nextElementToBeSent = next; // The next element is the actually read element
                    if (!hasNext) { // If we are at End-of-Stream
                        doCancel(); // We need to consider this `Subscription` as cancelled as per rule 1.6
                        subscriber.onComplete(); // Then we signal `onComplete` as per rule 1.2 and 1.5
                    }
                } while (!cancelled           // This makes sure that rule 1.8 is upheld, i.e. we need to stop signalling "eventually"
                         && --leftInBatch > 0 // This makes sure that we only send `batchSize` number of elements in one go (so we can yield to other Runnables)
                         && --demand > 0);    // This makes sure that rule 1.1 is upheld (sending more than was demanded)

                if (!cancelled && demand > 0) { // If the `Subscription` is still alive and well, and we have demand to satisfy, we signal ourselves to send more data
                    signal(Send.Instance);
                }
            } catch (final Throwable t) {
                // We can only getNextResult here if `onNext` or `onComplete` threw, and they are not allowed to according to 2.13, so we can only cancel and log here.
                doCancel(); // Make sure that we are cancelled, since we cannot do anything else since the `Subscriber` is faulty.
                (new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onNext or onComplete.", t)).printStackTrace(System.err);
            }
        }

        // This is a helper method to ensure that we always `cancel` when we signal `onError` as per rule 1.6
        private void terminateDueTo(final Throwable t) {
            cancelled = true; // When we signal onError, the subscription must be considered as cancelled, as per rule 1.6
            try {
                subscriber.onError(t); // Then we signal the error downstream, to the `Subscriber`
            } catch (final Throwable t2) { // If `onError` throws an exception, this is a spec violation according to rule 1.9, and all we can do is to log it.
                (new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
            }
        }

        // What `signal` does is that it sends signals to the `Subscription` asynchronously
        private void signal(final Signal signal) {
            if (inboundSignals.offer(signal)) { // No need to null-check here as ConcurrentLinkedQueue does this for us
                tryScheduleToExecute(); // Then we try to schedule it for execution, if it isn't already
            }
        }

        // This is the main "event loop" if you so will
        @Override
        public final void run() {
            if (on.get()) { // establishes a happens-before relationship with the end of the previous run
                try {
                    final Signal s = inboundSignals.poll(); // We take a signal off the queue
                    if (!cancelled) { // to make sure that we follow rule 1.8, 3.6 and 3.7
                        // Below we simply unpack the `Signal`s and invoke the corresponding methods
                        if (s instanceof Request) {
                            doRequest(((Request) s).n);
                        } else if (s == Send.Instance) {
                            doSend();
                        } else if (s == Cancel.Instance) {
                            doCancel();
                        } else if (s == Subscribe.Instance) {
                            doSubscribe();
                        }
                    }
                } finally {
                    on.set(false); // establishes a happens-before relationship with the beginning of the next run
                    if (!inboundSignals.isEmpty()) { // If we still have signals to process
                        tryScheduleToExecute(); // Then we try to schedule ourselves to execute again
                    }
                }
            }
        }

        // This method makes sure that this `Subscription` is only running on one Thread at a time,
        // this is important to make sure that we follow rule 1.3
        private final void tryScheduleToExecute() {
            if (on.compareAndSet(false, true)) {
                try {
                    executor.execute(this);
                } catch (Throwable t) { // If we can't run on the `Executor`, we need to fail gracefully
                    if (!cancelled) {
                        doCancel(); // First of all, this failure is not recoverable, so we need to follow rule 1.4 and 1.6
                        try {
                            terminateDueTo(new IllegalStateException("Publisher terminated due to unavailable Executor.", t));
                        } finally {
                            inboundSignals.clear(); // We're not going to need these anymore
                            // This subscription is cancelled by now, but letting it become schedulable again means
                            // that we can drain the inboundSignals queue if anything arrives after clearing
                            on.set(false);
                        }
                    }
                }
            }
        }

        // Our implementation of `Subscription.request` sends a signal to the Subscription that more elements are in demand
        @Override
        public void request(final long n) {
            signal(new Request(n));
        }

        // Our implementation of `Subscription.cancel` sends a signal to the Subscription that the `Subscriber` is not interested in any more elements
        @Override
        public void cancel() {
            signal(Cancel.Instance);
        }

        // The reason for the `executeQuery` method is that we want to ensure the `SubscriptionImpl`
        // is completely constructed before it is exposed to the thread pool, therefor this
        // method is only intended to be invoked once, and immediately after the constructor has
        // finished.
        void init() {
            signal(Subscribe.Instance);
        }
    }
}