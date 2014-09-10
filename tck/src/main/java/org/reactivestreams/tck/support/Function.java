package org.reactivestreams.tck.support;

public interface Function<In, Out> {
  public Out apply(In in) throws Throwable;
}