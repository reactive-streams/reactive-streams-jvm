/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

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
