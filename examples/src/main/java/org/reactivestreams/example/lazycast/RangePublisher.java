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

package org.reactivestreams.example.lazycast;

import org.reactivestreams.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A synchronous implementation of the {@link Publisher} that can
 * be subscribed to multiple times and each individual subscription
 * will receive range of monotonically increasing integer values on demand.
 */
public final class RangePublisher implements Publisher<Integer> {

    /** The starting value of the range. */
    final int start;

    /** The number of items to emit. */
    final int count;

    /**
     * Constructs a RangePublisher instance with the given start and count values
     * that yields a sequence of [start, start + count).
     * @param start the starting value of the range
     * @param count the number of items to emit
     */
    public RangePublisher(int start, int count) {
        this.start = start;
        this.count = count;
    }

    @Override
    public void subscribe(Subscriber<? super Integer> s) {
        s.onSubscribe(new RangeSubscription(s, start, start + count));
    }

    /**
     * A Subscription implementation that holds the current downstream
     * requested amount and responds to the downstream's request() and
     * cancel() calls.
     */
    static final class RangeSubscription extends AtomicLong implements Subscription {

        private static final long serialVersionUID = -9000845542177067735L;

        /** The Subscriber we are emitting integer values to. */
        final Subscriber<? super Integer> downstream;

        /** The end index (exclusive). */
        final int end;

        /**
         * The current index and within the [start, start + count) range that
         * will be emitted as downstream.onNext().
         */
        int index;

        /**
         * Indicates the emission should stop.
         */
        volatile boolean cancelled;

        /**
         * Holds onto the IllegalArgumentException (containing the offending stacktrace)
         * indicating there was a non-positive request() call from the downstream.
         */
        volatile Throwable invalidRequest;

        /**
         * Constructs a stateful RangeSubscription that emits signals to the given
         * downstream from an integer range of [start, end).
         * @param downstream the Subscriber receiving the integer values and the completion signal.
         * @param start the first integer value emitted, start of the range
         * @param end the end of the range, exclusive
         */
        RangeSubscription(Subscriber<? super Integer> downstream, int start, int end) {
            this.downstream = downstream;
            this.index = start;
            this.end = end;
        }

        @Override
        public void request(long n) {
            // Non-positive requests should be honored with IllegalArgumentException
            if (n <= 0L) {
                invalidRequest = new IllegalArgumentException("§3.9: non-positive requests are not allowed!");
                n = 1;
            }
            // Downstream requests are cumulative and may come from any thread
            for (;;) {
                long requested = get();
                long update = requested + n;
                // cap the amount at Long.MAX_VALUE
                if (update < 0L) {
                    update = Long.MAX_VALUE;
                }
                // atomically update the current requested amount
                if (compareAndSet(requested, update)) {
                    // if there was no prior request amount, we start the emission loop
                    if (requested == 0L) {
                        emit(update);
                    }
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            // Indicate to the emission loop it should stop.
            cancelled = true;
        }

        void emit(long currentRequested) {
            // Load fields to avoid re-reading them from memory due to volatile accesses in the loop.
            Subscriber<? super Integer> downstream = this.downstream;
            int index = this.index;
            int end = this.end;
            int emitted = 0;

            for (;;) {
                // Check if there was an invalid request and then report it.
                Throwable invalidRequest = this.invalidRequest;
                if (invalidRequest != null) {
                    downstream.onError(invalidRequest);
                    return;
                }

                // Loop while the index hasn't reached the end and we haven't
                // emitted all that's been requested
                while (index != end && emitted != currentRequested) {
                    // We stop if cancellation was requested
                    if (cancelled) {
                        return;
                    }

                    downstream.onNext(index);

                    // Increment the index for the next possible emission.
                    index++;
                    // Increment the emitted count to prevent overflowing the downstream.
                    emitted++;
                }

                // If the index reached the end, we complete the downstream.
                if (index == end) {
                    // Unless cancellation was requested by the last onNext.
                    if (!cancelled) {
                        downstream.onComplete();
                    }
                    return;
                }

                // Did the requested amount change while we were looping?
                long freshRequested = get();
                if (freshRequested == currentRequested) {
                    // Save where the loop has left off: the next value to be emitted
                    this.index = index;
                    // Atomically subtract the previously requested (also emitted) amount
                    currentRequested = addAndGet(-currentRequested);
                    // If there was no new request in between get() and addAndGet(), we simply quit
                    // The next 0 to N transition in request() will trigger the next emission loop.
                    if (currentRequested == 0L) {
                        break;
                    }
                    // Looks like there were more async requests, reset the emitted count and continue.
                    emitted = 0;
                } else {
                    // Yes, avoid the atomic subtraction and resume.
                    // emitted != currentRequest in this case and index
                    // still points to the next value to be emitted
                    currentRequested = freshRequested;
                }
            }
        }
    }
}
