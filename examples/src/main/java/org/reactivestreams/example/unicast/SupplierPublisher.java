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

package org.reactivestreams.example.unicast;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executor;

/**
 * SupplierPublisher uses a supplier with only one 'get()' method for supplying values
 * from unknown streams like files or sockets.
 * The supplier informs about the end of stream by returning a 'null' value.
 */
public class SupplierPublisher<T> extends AsyncIterablePublisher<T> {
    public SupplierPublisher(final Supplier<T> supplier, final Executor executor) {
        super(new Iterable<T>() {
          @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
              private T elem = supplier.get();
              @Override public boolean hasNext() { return elem != null; }
              @Override public T next() {
                if (!hasNext()) return Collections.<T>emptyList().iterator().next();
                else {
                    T prev = elem;
                    elem = supplier.get();
                    return prev;
                }
              }
              @Override public void remove() { throw new UnsupportedOperationException(); }
            };
          }
        }, executor);
    }

    static interface Supplier<T> {
        T get();
    }
}