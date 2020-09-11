/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.support;

import java.util.NoSuchElementException;

// simplest possible version of Scala's Option type
public abstract class Optional<T> {

  private static final Optional<Object> NONE = new Optional<Object>() {
    @Override
    public Object get() {
      throw new NoSuchElementException(".get call on None!");
    }

    @Override
    public boolean isEmpty() {
      return true;
    }
  };

  private Optional() {
  }

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> empty() {
    return (Optional<T>) NONE;
  }

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> of(T it) {
    if (it == null) return (Optional<T>) Optional.NONE;
    else return new Some(it);
  }

  public abstract T get();

  public abstract boolean isEmpty();

  public boolean isDefined() {
    return !isEmpty();
  }

  public static class Some<T> extends Optional<T> {
    private final T value;

    Some(T value) {
      this.value = value;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public String toString() {
      return String.format("Some(%s)", value);
    }
  }

  @Override
  public String toString() {
    return "None";
  }
}
