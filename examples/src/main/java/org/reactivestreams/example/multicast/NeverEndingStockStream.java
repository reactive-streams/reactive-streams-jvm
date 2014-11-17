package org.reactivestreams.example.multicast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulate a network connection that is firing data at you.
 * <p>
 * Purposefully not using the `Subscriber` and `Publisher` types to not confuse things with `StockPricePublisher`
 */
public class NeverEndingStockStream {

    private static final NeverEndingStockStream INSTANCE = new NeverEndingStockStream();

    private NeverEndingStockStream() {
    }

    // using array because it is far faster than list/set for iteration 
    // which is where most of the time in the tight loop will go (... well, beside object allocation)
    private volatile Handler[] handlers = new Handler[0];

    public static synchronized void addHandler(Handler handler) {
        if (INSTANCE.handlers.length == 0) {
            INSTANCE.handlers = new Handler[] { handler };
        } else {
            Handler[] newHandlers = new Handler[INSTANCE.handlers.length + 1];
            System.arraycopy(INSTANCE.handlers, 0, newHandlers, 0, INSTANCE.handlers.length);
            newHandlers[newHandlers.length - 1] = handler;
            INSTANCE.handlers = newHandlers;
        }
        INSTANCE.startIfNeeded();
    }

    public static synchronized void removeHandler(Handler handler) {
        // too lazy to do the array handling
        HashSet<Handler> set = new HashSet<Handler>(Arrays.asList(INSTANCE.handlers));
        set.remove(handler);
        INSTANCE.handlers = set.toArray(new Handler[set.size()]);
    }

    public static interface Handler {
        public void handle(Stock event);
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong stockIndex = new AtomicLong();

    private void startIfNeeded() {
        if (running.compareAndSet(false, true)) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    while (handlers.length > 0) {
                        long l = stockIndex.incrementAndGet();
                        Stock s = new Stock(l);
                        for (Handler h : handlers) {
                            h.handle(s);
                        }
                        try {
                            // just so it is someone sane how fast this is moving
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                        }
                    }
                    running.set(false);
                }

            }).start();
        }
    }

}
