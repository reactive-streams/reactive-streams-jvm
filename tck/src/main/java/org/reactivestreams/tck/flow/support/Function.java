/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.support;

public interface Function<In, Out> {
  public Out apply(In in) throws Throwable;
}
