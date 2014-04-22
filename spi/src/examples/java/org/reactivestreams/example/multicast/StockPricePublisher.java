package org.reactivestreams.example.multicast;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Handle;
import org.reactivestreams.Listener;
import org.reactivestreams.Source;
import org.reactivestreams.example.multicast.NeverEndingStockStream.Handler;

/**
 * Publisher of stock prices from a never ending stream.
 * <p>
 * It will share a single underlying stream with as many subscribers as it receives.
 * <p>
 * If the subscriber can not keep up, it will drop (different strategies could be implemented, configurable, etc).
 */
public class StockPricePublisher implements Source<Stock> {

    @Override
    public void listen(final Listener<Stock> s) {
        s.onListen(new Handle() {

            AtomicInteger capacity = new AtomicInteger();
            EventHandler handler = new EventHandler(s, capacity);

            @Override
            public void request(int n) {
                if (capacity.getAndAdd(n) == 0) {
                    // was at 0, so start up consumption again
                    startConsuming();
                }
            }

            @Override
            public void cancel() {
                System.out.println("StockPricePublisher => Cancel Subscription");
                NeverEndingStockStream.removeHandler(handler);
            }

            public void startConsuming() {
                NeverEndingStockStream.addHandler(handler);
            }

        });

    }

    private static final class EventHandler implements Handler {
        private final Listener<Stock> s;
        private final AtomicInteger capacity;

        private EventHandler(Listener<Stock> s, AtomicInteger capacity) {
            this.s = s;
            this.capacity = capacity;
        }

        @Override
        public void handle(Stock event) {
            int c = capacity.get();
            if (c == 0) {
                // shortcut instead of doing decrement/increment loops while no capacity
                return;
            }
            if (capacity.getAndDecrement() > 0) {
                s.onNext(event);
            } else {
                // we just decremented below 0 so increment back one
                capacity.incrementAndGet();
            }
        }
    }

}
