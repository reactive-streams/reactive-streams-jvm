package org.reactivestreams.tck.support;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executor;
import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.example.unicast.AsyncIterablePublisher;

public class HelperPublisher<T> extends AsyncIterablePublisher<T> {
  
    public HelperPublisher(final int from, final int to, final Function<Integer, T> create, final Executor executor) {
        super(new Iterable<T>() {
          { if(from > to) throw new IllegalArgumentException("from must be equal or greater than to!"); }
          @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
              private int at = from;
              @Override public boolean hasNext() { return at < to; }
              @Override public T next() {
                if (!hasNext()) return Collections.<T>emptyList().iterator().next();
                else try {
                  return create.apply(at++);
                } catch (Throwable t) {
                  throw new HelperPublisherException(
                    String.format("Failed to create element in %s for id %s!", getClass().getSimpleName(), at - 1), t);
                }
              }
              @Override public void remove() { throw new UnsupportedOperationException(); }
            };
          }
        }, executor);
    }
}