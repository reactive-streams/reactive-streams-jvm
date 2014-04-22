package org.reactivestreams.example.unicast;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Handle;
import org.reactivestreams.Listener;
import org.reactivestreams.Source;

class InfiniteIncrementNumberPublisher implements Source<Integer> {

    @Override
    public void listen(final Listener<Integer> s) {

        final AtomicInteger i = new AtomicInteger();

        Handle subscription = new Handle() {

            AtomicInteger capacity = new AtomicInteger();

            @Override
            public void request(int n) {
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

        s.onListen(subscription);

    }
}