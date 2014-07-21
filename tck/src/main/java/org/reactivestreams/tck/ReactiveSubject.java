package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public interface ReactiveSubject<I, O> extends Publisher<I>, Subscriber<O> {

}
