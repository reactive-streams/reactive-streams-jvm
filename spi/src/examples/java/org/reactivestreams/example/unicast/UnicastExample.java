package org.reactivestreams.example.unicast;

import org.reactivestreams.Source;

public class UnicastExample {

    /**
     * Each subscribe will start a new stream starting at 0.
     * 
     * @param args
     * @throws InterruptedException
     */
    public static void main(String... args) throws InterruptedException {
        Source<Integer> dataStream = new InfiniteIncrementNumberPublisher();

        dataStream.listen(new NumberSubscriberThatHopsThreads("A"));
        Thread.sleep(2000);
        dataStream.listen(new NumberSubscriberThatHopsThreads("B"));
    }

}
