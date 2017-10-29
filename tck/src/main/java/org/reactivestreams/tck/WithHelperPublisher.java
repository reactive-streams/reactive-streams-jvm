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

package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.flow.support.Function;
import org.reactivestreams.tck.flow.support.HelperPublisher;
import org.reactivestreams.tck.flow.support.InfiniteHelperPublisher;

import java.util.concurrent.ExecutorService;

/**
 * Type which is able to create elements based on a seed {@code id} value.
 * <p>
 * Simplest implementations will simply return the incoming id as the element.
 *
 * @param <T> type of element to be delivered to the Subscriber
 */
public abstract class WithHelperPublisher<T> {

  /** ExecutorService to be used by the provided helper {@link org.reactivestreams.Publisher} */
  public abstract ExecutorService publisherExecutorService();

  /**
   * Implement this method to match your expected element type.
   * In case of implementing a simple Subscriber which is able to consume any kind of element simply return the
   * incoming {@code element} element.
   * <p>
   * Sometimes the Subscriber may be limited in what type of element it is able to consume, this you may have to implement
   * this method such that the emitted element matches the Subscribers requirements. Simplest implementations would be
   * to simply pass in the {@code element} as payload of your custom element, such as appending it to a String or other identifier.
   * <p>
   * <b>Warning:</b> This method may be called concurrently by the helper publisher, thus it should be implemented in a
   * thread-safe manner.
   *
   * @return element of the matching type {@code T} that will be delivered to the tested Subscriber
   */
  public abstract T createElement(int element);

  /**
   * Helper method required for creating the Publisher to which the tested Subscriber will be subscribed and tested against.
   * <p>
   * By default an <b>asynchronously signalling Publisher</b> is provided, which will use {@link #createElement(int)}
   * to generate elements type your Subscriber is able to consume.
   * <p>
   * Sometimes you may want to implement your own custom custom helper Publisher - to validate behaviour of a Subscriber
   * when facing a synchronous Publisher for example. If you do, it MUST emit the exact number of elements asked for
   * (via the {@code elements} parameter) and MUST also must treat the following numbers of elements in these specific ways:
   * <ul>
   *   <li>
   *     If {@code elements} is {@code Long.MAX_VALUE} the produced stream must be infinite.
   *   </li>
   *   <li>
   *     If {@code elements} is {@code 0} the {@code Publisher} should signal {@code onComplete} immediatly.
   *     In other words, it should represent a "completed stream".
   *   </li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public Publisher<T> createHelperPublisher(long elements) {
    final Function<Integer, T> mkElement = new Function<Integer, T>() {
      @Override public T apply(Integer id) throws Throwable {
        return createElement(id);
      }
    };

    if (elements > Integer.MAX_VALUE) return new InfiniteHelperPublisher(mkElement, publisherExecutorService());
    else return new HelperPublisher(0, (int) elements, mkElement, publisherExecutorService());
  }

}
