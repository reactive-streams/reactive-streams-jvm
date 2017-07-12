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
 * Bridge between Reactive-Streams API and the Java 9 Flow API.
 */
public final class ReactiveFlowBridge {
    /** Utility class. */
    private ReactiveFlowBridge() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Converts a Flow Publisher into a Reactive Publisher.
     * @param <T> the value type
     * @param flowPublisher the source Flow Publisher to convert
     * @return the equivalent Reactive Publisher
     */
    @SuppressWarnings("unchecked")
    public static <T> org.reactivestreams.Publisher<T> toReactive(
            Flow.Publisher<? extends T> flowPublisher) {
        if (flowPublisher == null) {
            throw new NullPointerException("flowPublisher");
        }
        if (flowPublisher instanceof org.reactivestreams.Publisher) {
            return (org.reactivestreams.Publisher<T>)flowPublisher;
        }
        if (flowPublisher instanceof FlowPublisherFromReactive) {
            return (org.reactivestreams.Publisher<T>)(((FlowPublisherFromReactive<T>)flowPublisher).reactive);
        }
        return new ReactivePublisherFromFlow<T>(flowPublisher);
    }

    /**
     * Converts a Reactive Publisher into a Flow Publisher.
     * @param <T> the value type
     * @param reactivePublisher the source Reactive Publisher to convert
     * @return the equivalent Flow Publisher
     */
    @SuppressWarnings("unchecked")
    public static <T> Flow.Publisher<T> toFlow(
            org.reactivestreams.Publisher<? extends T> reactivePublisher
    ) {
        if (reactivePublisher == null) {
            throw new NullPointerException("reactivePublisher");
        }
        if (reactivePublisher instanceof Flow.Publisher) {
            return (Flow.Publisher<T>)reactivePublisher;
        }
        if (reactivePublisher instanceof ReactivePublisherFromFlow) {
            return (Flow.Publisher<T>)(((ReactivePublisherFromFlow<T>)reactivePublisher).flow);
        }
        return new FlowPublisherFromReactive<T>(reactivePublisher);
    }
    
    /**
     * Converts a Flow Processor into a Reactive Processor.
     * @param <T> the input value type
     * @param <U> the output value type
     * @param flowProcessor the source Flow Processor to convert
     * @return the equivalent Reactive Processor
     */
    @SuppressWarnings("unchecked")
    public static <T, U> org.reactivestreams.Processor<T, U> toReactive(
            Flow.Processor<? super T, ? extends U> flowProcessor
    ) {
        if (flowProcessor == null) {
            throw new NullPointerException("flowProcessor");
        }
        if (flowProcessor instanceof org.reactivestreams.Processor) {
            return (org.reactivestreams.Processor<T, U>)flowProcessor;
        }
        if (flowProcessor instanceof FlowToReactiveProcessor) {
            return (org.reactivestreams.Processor<T, U>)(((FlowToReactiveProcessor<T, U>)flowProcessor).reactive);
        }
        return new ReactiveToFlowProcessor<T, U>(flowProcessor);
    }

    /**
     * Converts a Reactive Processor into a Flow Processor.
     * @param <T> the input value type
     * @param <U> the output value type
     * @param reactiveProcessor the source Reactive Processor to convert
     * @return the equivalent Flow Processor
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Flow.Processor<T, U> toFlow(
            org.reactivestreams.Processor<? super T, ? extends U> reactiveProcessor
        ) {
        if (reactiveProcessor == null) {
            throw new NullPointerException("reactiveProcessor");
        }
        if (reactiveProcessor instanceof Flow.Processor) {
            return (Flow.Processor<T, U>)reactiveProcessor;
        }
        if (reactiveProcessor instanceof ReactiveToFlowProcessor) {
            return (Flow.Processor<T, U>)(((ReactiveToFlowProcessor<T, U>)reactiveProcessor).flow);
        }
        return new FlowToReactiveProcessor<T, U>(reactiveProcessor);
    }
    
    /**
     * Wraps a Reactive Subscription and converts the calls to a Flow Subscription.
     */
    static final class FlowToReactiveSubscription implements Flow.Subscription {
        private final org.reactivestreams.Subscription reactive;
        
        public FlowToReactiveSubscription(org.reactivestreams.Subscription reactive) {
            this.reactive = reactive;
        }

        @Override
        public void request(long n) {
            reactive.request(n);
        }

        @Override
        public void cancel() {
            reactive.cancel();
        }
        
    }
    
    /**
     * Wraps a Flow Subscription and converts the calls to a Reactive Subscription.
     */
    static final class ReactiveToFlowSubscription implements org.reactivestreams.Subscription {
        private final Flow.Subscription flow;
        
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
     * Wraps a Reactive Subscriber and forwards methods of the Flow Subscriber to it.
     * @param <T> the value type
     */
    static final class FlowToReactiveSubscriber<T> 
            implements Flow.Subscriber<T> {
        private final org.reactivestreams.Subscriber<? super T> reactive;
        
        public FlowToReactiveSubscriber(org.reactivestreams.Subscriber<? super T> reactive) {
            this.reactive = reactive;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            reactive.onSubscribe(new ReactiveToFlowSubscription(subscription));
        }

        @Override
        public void onNext(T item) {
            reactive.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            reactive.onError(throwable);
        }

        @Override
        public void onComplete() {
            reactive.onComplete();
        }
        
    }

    /**
     * Wraps a Reactive Subscriber and forwards methods of the Flow Subscriber to it.
     * @param <T> the value type
     */
    static final class ReactiveToFlowSubscriber<T> 
            implements org.reactivestreams.Subscriber<T> {
        private final Flow.Subscriber<? super T> flow;
        
        public ReactiveToFlowSubscriber(Flow.Subscriber<? super T> flow) {
            this.flow = flow;
        }

        @Override
        public void onSubscribe(org.reactivestreams.Subscription subscription) {
            flow.onSubscribe(new FlowToReactiveSubscription(subscription));
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
     * Wraps a Flow Processor and forwards methods of the Reactive Processor to it.
     * @param <T> the input type
     * @param <U> the output type
     */
    static final class ReactiveToFlowProcessor<T, U>
            implements org.reactivestreams.Processor<T, U> {
        private final Flow.Processor<? super T, ? extends U> flow;
        
        public ReactiveToFlowProcessor(Flow.Processor<? super T, ? extends U> flow) {
            this.flow = flow;
        }

        @Override
        public void onSubscribe(org.reactivestreams.Subscription s) {
            flow.onSubscribe(new FlowToReactiveSubscription(s));
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
            if (s == null) {
                flow.subscribe(null);
                return;
            }
            flow.subscribe(new FlowToReactiveSubscriber<U>(s));
        }
    }
    
    /**
     * Wraps a Reactive Processor and forwards methods of the Flow Processor to it.
     * @param <T> the input type
     * @param <U> the output type
     */
    static final class FlowToReactiveProcessor<T, U>
            implements Flow.Processor<T, U> {
        private final org.reactivestreams.Processor<? super T, ? extends U> reactive;
        
        public FlowToReactiveProcessor(org.reactivestreams.Processor<? super T, ? extends U> reactive) {
            this.reactive = reactive;
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            reactive.onSubscribe(new ReactiveToFlowSubscription(s));
        }

        @Override
        public void onNext(T t) {
            reactive.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            reactive.onError(t);
        }

        @Override
        public void onComplete() {
            reactive.onComplete();
        }

        @Override
        public void subscribe(Flow.Subscriber<? super U> s) {
            if (s == null) {
                reactive.subscribe(null);
                return;
            }
            reactive.subscribe(new ReactiveToFlowSubscriber<U>(s));
        }
    }

    /**
     * Reactive Publisher that wraps a Flow Publisher.
     * @param <T> the value type
     */
    static final class ReactivePublisherFromFlow<T> implements org.reactivestreams.Publisher<T> {

        final Flow.Publisher<? extends T> flow;

        public ReactivePublisherFromFlow(Flow.Publisher<? extends T> flowPublisher) {
            this.flow = flowPublisher;
        }

        @Override
        public void subscribe(org.reactivestreams.Subscriber<? super T> reactive) {
            if (reactive == null) {
                flow.subscribe(null);
                return;
            }
            flow.subscribe(new FlowToReactiveSubscriber<T>(reactive));
        }
    }

    /**
     * Flow Publisher that wraps a Reactive Publisher.
     * @param <T> the value type
     */
    static final class FlowPublisherFromReactive<T> implements Flow.Publisher<T> {

        final org.reactivestreams.Publisher<? extends T> reactive;

        public FlowPublisherFromReactive(org.reactivestreams.Publisher<? extends T> reactivePublisher) {
            this.reactive = reactivePublisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super T> flow) {
            if (flow == null) {
                reactive.subscribe(null);
                return;
            }
            reactive.subscribe(new ReactiveToFlowSubscriber<T>(flow));
        }
    }

}