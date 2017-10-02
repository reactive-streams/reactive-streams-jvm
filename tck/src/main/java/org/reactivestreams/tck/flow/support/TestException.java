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

/**
 * Exception used by the TCK to signal failures.
 * May be thrown or signalled through {@link org.reactivestreams.Subscriber#onError(Throwable)}.
 */
public final class TestException extends RuntimeException {
  public TestException() {
    super("Test Exception: Boom!");
  }
}
