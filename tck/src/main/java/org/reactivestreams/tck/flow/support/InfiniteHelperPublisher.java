/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.support;

import org.reactivestreams.example.unicast.AsyncIterablePublisher;

import java.util.Iterator;
import java.util.concurrent.Executor;

public class InfiniteHelperPublisher<T> extends AsyncIterablePublisher<T> {

    public InfiniteHelperPublisher(final Function<Integer, T> create, final Executor executor) {
        super(new Iterable<T>() {
          @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
              private int at = 0;

              @Override public boolean hasNext() { return true; }
              @Override public T next() {
                try {
                  return create.apply(at++); // Wraps around on overflow
                } catch (Throwable t) {
                  throw new IllegalStateException(
                    String.format("Failed to create element in %s for id %s!", getClass().getSimpleName(), at - 1), t);
                }
              }
              @Override public void remove() { throw new UnsupportedOperationException(); }
            };
          }
        }, executor);
    }
}
