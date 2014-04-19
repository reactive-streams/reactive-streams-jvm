package org.reactivestreams.example.multicast;

public class Stock {

    private final long l;

    public Stock(long l) {
        this.l = l;
    }
    
    public long getPrice() {
        return l;
    }

}
