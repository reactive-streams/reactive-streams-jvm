package org.reactivestreams.example.multicast;

import org.reactivestreams.Publisher;

public class MulticastExample {

    /**
     * Each subscribe will join an existing stream.
     * 
     * @param args
     * @throws InterruptedException
     */
    public static void main(String... args) throws InterruptedException {
        Publisher<Stock> dataStream = new StockPricePublisher();

        dataStream.subscribe(new StockPriceSubscriber(5, 500)); // 500ms on each event, infinite
        dataStream.subscribe(new StockPriceSubscriber(10, 2000)); // 2000ms on each event, infinite
        Thread.sleep(5000);
        dataStream.subscribe(new StockPriceSubscriber(10, 111, 20)); // 111ms on each event, take 20
        Thread.sleep(5000);
        dataStream.subscribe(new StockPriceSubscriber(10, 222, 20));// 222ms on each event, take 20
    }
}
