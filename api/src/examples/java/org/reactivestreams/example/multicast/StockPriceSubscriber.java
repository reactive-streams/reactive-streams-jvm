package org.reactivestreams.example.multicast;

import java.util.concurrent.ArrayBlockingQueue;

import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;

public class StockPriceSubscriber implements Subscriber<Stock> {

    private final ArrayBlockingQueue<Stock> buffer;
    private final int delayPerStock;
    private volatile boolean terminated = false;
    private final int take;

    public StockPriceSubscriber(int bufferSize, int delayPerStock, int take) {
        this.buffer = new ArrayBlockingQueue<Stock>(bufferSize);
        this.delayPerStock = delayPerStock;
        this.take = take;
    }

    public StockPriceSubscriber(int bufferSize, int delayPerStock) {
        this(bufferSize, delayPerStock, -1);
    }

    @Override
    public void onSubscribe(Subscription s) {
        System.out.println("StockPriceSubscriber.onSubscribe => request " + buffer.remainingCapacity());
        s.request(buffer.remainingCapacity());
        startAsyncWork(s);
    }

    @Override
    public void onNext(Stock t) {
        buffer.add(t);
    }

    @Override
    public void onError(Throwable t) {
        terminated = true;
        throw new RuntimeException(t);
    }

    @Override
    public void onComplete() {
        terminated = true;
    }

    private void startAsyncWork(final Subscription s) {
        System.out.println("StockPriceSubscriber => Start new worker thread");
        /* don't write real code like this! just for quick demo */
        new Thread(new Runnable() {
            public void run() {
                int received = 0;

                while (!terminated) {
                    Stock v = buffer.poll();
                    try {
                        Thread.sleep(delayPerStock);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (buffer.size() < 3) {
                        s.request(buffer.remainingCapacity());
                    }
                    if (v != null) {
                        received++;
                        System.out.println("StockPriceSubscriber[" + delayPerStock + "] => " + v.getPrice());
                        if (take > 0 && received >= take) {
                            s.cancel();
                            terminated = true;
                        }
                    }
                }
            }
        }).start();
    }

}
