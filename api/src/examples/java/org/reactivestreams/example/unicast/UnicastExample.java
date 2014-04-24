package org.reactivestreams.example.unicast;

import org.reactivestreams.Publisher;

public class UnicastExample {

    /**
     * Each subscribe will start a new stream starting at 0.
     * 
     * @param args
     * @throws InterruptedException
     */
    public static void main(String... args) throws InterruptedException {
        Publisher<Integer> dataStream = new InfiniteIncrementNumberPublisher();

        dataStream.subscribe(new NumberSubscriberThatHopsThreads("A"));
        Thread.sleep(2000);
        dataStream.subscribe(new NumberSubscriberThatHopsThreads("B"));
    }

}
