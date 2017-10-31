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

import java.util.concurrent.Flow;

/**
 * Bridge between Reactive Streams API and the Java 9 {@link java.util.concurrent.Flow} API.
 */
public final class ReactiveStreamsFlowBridge {
    /** Utility class. */
    private ReactiveStreamsFlowBridge() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Converts a Flow Publisher into a Reactive Streams Publisher.
     * @param <T> the element type
     * @param flowPublisher the source Flow Publisher to convert
     * @return the equivalent Reactive Streams Publisher
     */
    @SuppressWarnings("unchecked")
    public static <T> org.reactivestreams.Publisher<T> toPublisher(
            Flow.Publisher<? extends T> flowPublisher) {
        if (flowPublisher == null) {
            throw new NullPointerException("flowPublisher");
        }
        if (flowPublisher instanceof FlowPublisherFromReactive) {
            return (org.reactivestreams.Publisher<T>)(((FlowPublisherFromReactive<T>)flowPublisher).reactiveStreams);
        }
        if (flowPublisher instanceof org.reactivestreams.Publisher) {
            return (org.reactivestreams.Publisher<T>)flowPublisher;
        }
        return new ReactivePublisherFromFlow<T>(flowPublisher);
    }

    /**
     * Converts a Reactive Streams Publisher into a Flow Publisher.
     * @param <T> the element type
     * @param reactiveStreamsPublisher the source Reactive Streams Publisher to convert
     * @return the equivalent Flow Publisher
     */
    @SuppressWarnings("unchecked")
    public static <T> Flow.Publisher<T> toFlowPublisher(
            org.reactivestreams.Publisher<? extends T> reactiveStreamsPublisher
    ) {
        if (reactiveStreamsPublisher == null) {
            throw new NullPointerException("reactiveStreamsPublisher");
        }
        if (reactiveStreamsPublisher instanceof ReactivePublisherFromFlow) {
            return (Flow.Publisher<T>)(((ReactivePublisherFromFlow<T>)reactiveStreamsPublisher).flow);
        }
        if (reactiveStreamsPublisher instanceof Flow.Publisher) {
            return (Flow.Publisher<T>)reactiveStreamsPublisher;
        }
        return new FlowPublisherFromReactive<T>(reactiveStreamsPublisher);
    }

    /**
     * Converts a Flow Processor into a Reactive Streams Processor.
     * @param <T> the input value type
     * @param <U> the output value type
     * @param flowProcessor the source Flow Processor to convert
     * @return the equivalent Reactive Streams Processor
     */
    @SuppressWarnings("unchecked")
    public static <T, U> org.reactivestreams.Processor<T, U> toProcessor(
            Flow.Processor<? super T, ? extends U> flowProcessor
    ) {
        if (flowProcessor == null) {
            throw new NullPointerException("flowProcessor");
        }
        if (flowProcessor instanceof FlowToReactiveProcessor) {
            return (org.reactivestreams.Processor<T, U>)(((FlowToReactiveProcessor<T, U>)flowProcessor).reactiveStreams);
        }
        if (flowProcessor instanceof org.reactivestreams.Processor) {
            return (org.reactivestreams.Processor<T, U>)flowProcessor;
        }
        return new ReactiveToFlowProcessor<T, U>(flowProcessor);
    }

    /**
     * Converts a Reactive Streams Processor into a Flow Processor.
     * @param <T> the input value type
     * @param <U> the output value type
     * @param reactiveStreamsProcessor the source Reactive Streams Processor to convert
     * @return the equivalent Flow Processor
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Flow.Processor<T, U> toFlowProcessor(
            org.reactivestreams.Processor<? super T, ? extends U> reactiveStreamsProcessor
        ) {
        if (reactiveStreamsProcessor == null) {
            throw new NullPointerException("reactiveStreamsProcessor");
        }
        if (reactiveStreamsProcessor instanceof ReactiveToFlowProcessor) {
            return (Flow.Processor<T, U>)(((ReactiveToFlowProcessor<T, U>)reactiveStreamsProcessor).flow);
        }
        if (reactiveStreamsProcessor instanceof Flow.Processor) {
            return (Flow.Processor<T, U>)reactiveStreamsProcessor;
        }
        return new FlowToReactiveProcessor<T, U>(reactiveStreamsProcessor);
    }

    /**
     * Converts a Reactive Streams Subscriber into a Flow Subscriber.
     * @param <T> the input and output value type
     * @param reactiveStreamsSubscriber the Reactive Streams Subscriber instance to convert
     * @return the equivalent Flow Subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Flow.Subscriber<T> toFlowSubscriber(org.reactivestreams.Subscriber<T> reactiveStreamsSubscriber) {
        if (reactiveStreamsSubscriber == null) {
            throw new NullPointerException("reactiveStreamsSubscriber");
        }
        if (reactiveStreamsSubscriber instanceof ReactiveToFlowSubscriber) {
            return (Flow.Subscriber<T>)((ReactiveToFlowSubscriber<T>)reactiveStreamsSubscriber).flow;
        }
        if (reactiveStreamsSubscriber instanceof Flow.Subscriber) {
            return (Flow.Subscriber<T>)reactiveStreamsSubscriber;
        }
        return new FlowToReactiveSubscriber<T>(reactiveStreamsSubscriber);
    }

    /**
     * Converts a Flow Subscriber into a Reactive Streams Subscriber.
     * @param <T> the input and output value type
     * @param flowSubscriber the Flow Subscriber instance to convert
     * @return the equivalent Reactive Streams Subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> org.reactivestreams.Subscriber<T> toSubscriber(Flow.Subscriber<T> flowSubscriber) {
        if (flowSubscriber == null) {
            throw new NullPointerException("flowSubscriber");
        }
        if (flowSubscriber instanceof FlowToReactiveSubscriber) {
            return (org.reactivestreams.Subscriber<T>)((FlowToReactiveSubscriber<T>)flowSubscriber).reactiveStreams;
        }
        if (flowSubscriber instanceof org.reactivestreams.Subscriber) {
            return (org.reactivestreams.Subscriber<T>)flowSubscriber;
        }
        return new ReactiveToFlowSubscriber<T>(flowSubscriber);
    }

    /**
     * Wraps a Reactive Streams Subscription and converts the calls to a Flow Subscription.
     */
    static final class FlowToReactiveSubscription implements Flow.Subscription {
        final org.reactivestreams.Subscription reactiveStreams;

        public FlowToReactiveSubscription(org.reactivestreams.Subscription reactive) {
            this.reactiveStreams = reactive;
        }

        @Override
        public void request(long n) {
            reactiveStreams.request(n);
        }

        @Override
        public void cancel() {
            reactiveStreams.cancel();
        }

    }

    /**
     * Wraps a Flow Subscription and converts the calls to a Reactive Streams Subscription.
     */
    static final class ReactiveToFlowSubscription implements org.reactivestreams.Subscription {
        final Flow.Subscription flow;

        public ReactiveToFlowSubscription(Flow.Subscription flow) {
            this.flow = flow;
        }

        @Override
        public void request(long n) {
            flow.request(n);
        }

        @Override
        public void cancel() {
            flow.cancel();
        }


    }

    /**
     * Wraps a Reactive Streams Subscriber and forwards methods of the Flow Subscriber to it.
     * @param <T> the element type
     */
    static final class FlowToReactiveSubscriber<T>
            implements Flow.Subscriber<T> {
        final org.reactivestreams.Subscriber<? super T> reactiveStreams;

        public FlowToReactiveSubscriber(org.reactivestreams.Subscriber<? super T> reactive) {
            this.reactiveStreams = reactive;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            reactiveStreams.onSubscribe((subscription == null) ? null : new ReactiveToFlowSubscription(subscription));
        }

        @Override
        public void onNext(T item) {
            reactiveStreams.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            reactiveStreams.onError(throwable);
        }

        @Override
        public void onComplete() {
            reactiveStreams.onComplete();
        }

    }

    /**
     * Wraps a Reactive Streams Subscriber and forwards methods of the Flow Subscriber to it.
     * @param <T> the element type
     */
    static final class ReactiveToFlowSubscriber<T>
            implements org.reactivestreams.Subscriber<T> {
        final Flow.Subscriber<? super T> flow;

        public ReactiveToFlowSubscriber(Flow.Subscriber<? super T> flow) {
            this.flow = flow;
        }

        @Override
        public void onSubscribe(org.reactivestreams.Subscription subscription) {
            flow.onSubscribe((subscription == null) ? null : new FlowToReactiveSubscription(subscription));
        }

        @Override
        public void onNext(T item) {
            flow.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            flow.onError(throwable);
        }

        @Override
        public void onComplete() {
            flow.onComplete();
        }

    }

    /**
     * Wraps a Flow Processor and forwards methods of the Reactive Streams Processor to it.
     * @param <T> the input type
     * @param <U> the output type
     */
    static final class ReactiveToFlowProcessor<T, U>
            implements org.reactivestreams.Processor<T, U> {
        final Flow.Processor<? super T, ? extends U> flow;

        public ReactiveToFlowProcessor(Flow.Processor<? super T, ? extends U> flow) {
            this.flow = flow;
        }

        @Override
        public void onSubscribe(org.reactivestreams.Subscription subscription) {
            flow.onSubscribe((subscription == null) ? null : new FlowToReactiveSubscription(subscription));
        }

        @Override
        public void onNext(T t) {
            flow.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            flow.onError(t);
        }

        @Override
        public void onComplete() {
            flow.onComplete();
        }

        @Override
        public void subscribe(org.reactivestreams.Subscriber<? super U> s) {
            flow.subscribe((s == null) ? null : new FlowToReactiveSubscriber<U>(s));
        }
    }

    /**
     * Wraps a Reactive Streams Processor and forwards methods of the Flow Processor to it.
     * @param <T> the input type
     * @param <U> the output type
     */
    static final class FlowToReactiveProcessor<T, U>
            implements Flow.Processor<T, U> {
        final org.reactivestreams.Processor<? super T, ? extends U> reactiveStreams;

        public FlowToReactiveProcessor(org.reactivestreams.Processor<? super T, ? extends U> reactive) {
            this.reactiveStreams = reactive;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            reactiveStreams.onSubscribe((subscription == null) ? null : new ReactiveToFlowSubscription(subscription));
        }

        @Override
        public void onNext(T t) {
            reactiveStreams.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            reactiveStreams.onError(t);
        }

        @Override
        public void onComplete() {
            reactiveStreams.onComplete();
        }

        @Override
        public void subscribe(Flow.Subscriber<? super U> s) {
            reactiveStreams.subscribe((s == null) ? null : new ReactiveToFlowSubscriber<U>(s));
        }
    }

    /**
     * Reactive Streams Publisher that wraps a Flow Publisher.
     * @param <T> the element type
     */
    static final class ReactivePublisherFromFlow<T> implements org.reactivestreams.Publisher<T> {

        final Flow.Publisher<? extends T> flow;

        public ReactivePublisherFromFlow(Flow.Publisher<? extends T> flowPublisher) {
            this.flow = flowPublisher;
        }

        @Override
        public void subscribe(org.reactivestreams.Subscriber<? super T> reactive) {
            flow.subscribe((reactive == null) ? null : new FlowToReactiveSubscriber<T>(reactive));
        }
    }

    /**
     * Flow Publisher that wraps a Reactive Streams Publisher.
     * @param <T> the element type
     */
    static final class FlowPublisherFromReactive<T> implements Flow.Publisher<T> {

        final org.reactivestreams.Publisher<? extends T> reactiveStreams;

        public FlowPublisherFromReactive(org.reactivestreams.Publisher<? extends T> reactivePublisher) {
            this.reactiveStreams = reactivePublisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super T> flow) {
            reactiveStreams.subscribe((flow == null) ? null : new ReactiveToFlowSubscriber<T>(flow));
        }
    }

}
