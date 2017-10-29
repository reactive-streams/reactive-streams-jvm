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

public final class SubscriberBufferOverflowException extends RuntimeException {
  public SubscriberBufferOverflowException() {
  }

  public SubscriberBufferOverflowException(String message) {
    super(message);
  }

  public SubscriberBufferOverflowException(String message, Throwable cause) {
    super(message, cause);
  }

  public SubscriberBufferOverflowException(Throwable cause) {
    super(cause);
  }
}
