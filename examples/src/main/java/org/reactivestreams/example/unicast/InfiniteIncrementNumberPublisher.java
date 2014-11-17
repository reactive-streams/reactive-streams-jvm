package org.reactivestreams.example.unicast;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Publisher;

class InfiniteIncrementNumberPublisher implements Publisher<Integer> {

    @Override
    public void subscribe(final Subscriber<? super Integer> s) {

        final AtomicInteger i = new AtomicInteger();

        Subscription subscription = new Subscription() {

            AtomicLong capacity = new AtomicLong();

            @Override
            public void request(long n) {
                System.out.println("signalAdditionalDemand => " + n);
                if (capacity.getAndAdd(n) == 0) {
                    // start sending again if it wasn't already running
                    send();
                }
            }

            private void send() {
                System.out.println("send => " + capacity.get());
                // this would normally use an eventloop, actor, whatever
                new Thread(new Runnable() {

                    public void run() {
                        do {
                            s.onNext(i.incrementAndGet());
                        } while (capacity.decrementAndGet() > 0);
                    }
                }).start();
            }

            @Override
            public void cancel() {
                capacity.set(-1);
            }

        };

        s.onSubscribe(subscription);

    }
}