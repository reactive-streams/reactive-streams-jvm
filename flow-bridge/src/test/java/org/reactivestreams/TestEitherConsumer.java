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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Class that provides basic state assertions on received elements and
 * terminal signals from either a Reactive Streams Publisher or a
 * Flow Publisher.
 * <p>
 *     As with standard {@link Subscriber}s, an instance of this class
 *     should be subscribed to at most one (Reactive Streams or
 *     Flow) Publisher.
 * </p>
 * @param <T> the element type
 */
class TestEitherConsumer<T> implements Flow.Subscriber<T>, Subscriber<T> {

    protected final List<T> values;

    protected final List<Throwable> errors;

    protected int completions;

    protected Flow.Subscription subscription;

    protected Subscription subscriptionRs;

    protected final CountDownLatch done;

    final long initialRequest;

    public TestEitherConsumer() {
        this(Long.MAX_VALUE);
    }

    public TestEitherConsumer(long initialRequest) {
        this.values = new ArrayList<T>();
        this.errors = new ArrayList<Throwable>();
        this.done = new CountDownLatch(1);
        this.initialRequest = initialRequest;
    }

    @Override
    public final void onSubscribe(Flow.Subscription s) {
        this.subscription = s;
        s.request(initialRequest);
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscriptionRs = s;
        s.request(initialRequest);
    }

    @Override
    public void onNext(T item) {
        values.add(item);
        if (subscription == null && subscriptionRs == null) {
            errors.add(new IllegalStateException("onSubscribe not called"));
        }
    }

    @Override
    public void onError(Throwable throwable) {
        errors.add(throwable);
        if (subscription == null && subscriptionRs == null) {
            errors.add(new IllegalStateException("onSubscribe not called"));
        }
        done.countDown();
    }

    @Override
    public void onComplete() {
        completions++;
        if (subscription == null && subscriptionRs == null) {
            errors.add(new IllegalStateException("onSubscribe not called"));
        }
        done.countDown();
    }

    public final void cancel() {
        // FIXME implement deferred cancellation
    }

    public final List<T> values() {
        return values;
    }

    public final List<Throwable> errors() {
        return errors;
    }

    public final int completions() {
        return completions;
    }

    public final boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return done.await(timeout, unit);
    }

    public final TestEitherConsumer<T> assertResult(T... items) {
        if (!values.equals(Arrays.asList(items))) {
            throw new AssertionError("Expected: " + Arrays.toString(items) + ", Actual: " + values + ", Completions: " + completions);
        }
        if (completions != 1) {
            throw new AssertionError("Not completed: " + completions);
        }
        return this;
    }


    public final TestEitherConsumer<T> assertFailure(Class<? extends Throwable> errorClass, T... items) {
        if (!values.equals(Arrays.asList(items))) {
            throw new AssertionError("Expected: " + Arrays.toString(items) + ", Actual: " + values + ", Completions: " + completions);
        }
        if (completions != 0) {
            throw new AssertionError("Completed: " + completions);
        }
        if (errors.isEmpty()) {
            throw new AssertionError("No errors");
        }
        if (!errorClass.isInstance(errors.get(0))) {
            AssertionError ae = new AssertionError("Wrong throwable");
            ae.initCause(errors.get(0));
            throw ae;
        }
        return this;
    }

    public final TestEitherConsumer<T> awaitDone(long timeout, TimeUnit unit) {
        try {
            if (!done.await(timeout, unit)) {
                subscription.cancel();
                throw new RuntimeException("Timed out. Values: " + values.size()
                        + ", Errors: " + errors.size() + ", Completions: " + completions);
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interrupted");
        }
        return this;
    }

    public final TestEitherConsumer<T> assertRange(int start, int count) {
        if (values.size() != count) {
            throw new AssertionError("Expected: " + count + ", Actual: " + values.size());
        }
        for (int i = 0; i < count; i++) {
            if ((Integer)values.get(i) != start + i) {
                throw new AssertionError("Index: " + i + ", Expected: "
                        + (i + start) + ", Actual: " +values.get(i));
            }
        }
        if (completions != 1) {
            throw new AssertionError("Not completed: " + completions);
        }
        return this;
    }
}