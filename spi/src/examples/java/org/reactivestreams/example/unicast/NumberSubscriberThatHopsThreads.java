package org.reactivestreams.example.unicast;

import java.util.concurrent.ArrayBlockingQueue;

import org.reactivestreams.Handle;
import org.reactivestreams.Listener;

class NumberSubscriberThatHopsThreads implements Listener<Integer> {

    final int BUFFER_SIZE = 10;
    private final ArrayBlockingQueue<Integer> buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);
    private volatile boolean terminated = false;
    private final String token;

    NumberSubscriberThatHopsThreads(String token) {
        this.token = token;
    }

    @Override
    public void onListen(Handle s) {
        System.out.println("onSubscribe => request " + BUFFER_SIZE);
        s.request(BUFFER_SIZE);
        startAsyncWork(s);
    }

    @Override
    public void onNext(Integer t) {
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

    private void startAsyncWork(final Handle s) {
        System.out.println("**** Start new worker thread");
        /* don't write real code like this! just for quick demo */
        new Thread(new Runnable() {
            public void run() {
                while (!terminated) {
                    Integer v = buffer.poll();
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (buffer.size() < 3) {
                        s.request(BUFFER_SIZE - buffer.size());
                    }
                    if (v != null) {
                        System.out.println(token + " => Did stuff with v: " + v);
                    }
                }
            }
        }).start();
    }
}