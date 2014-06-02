# Reactive Streams #

The purpose of Reactive Streams is to provide a standard for asynchronous stream processing with non-blocking backpressure.

The latest preview release is available on Maven Central as

    <dependency>
      <groupId>org.reactivestreams</groupId>
      <artifactId>reactive-streams</artifactId>
      <version>0.4.M1</version>
    </dependency>
    <dependency>
      <groupId>org.reactivestreams</groupId>
      <artifactId>reactive-streams-tck</artifactId>
      <version>0.4.M1</version>
    </dependency>

## Goals, Design and Scope ##

Handling streams of data—especially “live” data whose volume is not predetermined—requires special care in an asynchronous system. The most prominent issue is that resource consumption needs to be carefully controlled such that a fast data source does not overwhelm the stream destination. Asynchrony is needed in order to enable the parallel use of computing resources, on collaborating network hosts or multiple CPU cores within a single machine.

The main goal of Reactive Streams is to govern the exchange of stream data across an asynchronous boundary – think passing  elements on to another thread or thread-pool — while ensuring that the receiving side is not forced to buffer arbitrary amounts of data. In other words, backpressure is an integral part of this model in order to allow the queues which mediate between threads to be bounded. The benefits of asynchronous processing would be negated if the communication of backpressure were synchronous (see also the [Reactive Manifesto](http://reactivemanifesto.org/)), therefore care has been taken to mandate fully non-blocking and asynchronous behavior of all aspects of a Reactive Streams implementation.

It is the intention of this specification to allow the creation of many conforming implementations, which by virtue of abiding by the rules will be able to interoperate smoothly, preserving the aforementioned benefits and characteristics across the whole processing graph of a stream application.

It should be noted that the precise nature of stream manipulations (transformation, splitting, merging, etc.) is not covered by this specification. Reactive Streams are only concerned with mediating the stream of data between different processing elements. In their development care has been taken to ensure that all basic ways of combining streams can be expressed.

In summary, Reactive Streams is a standard and specification for Stream-oriented libraries for the JVM that

 - process a potentially unbounded number of elements 
 - in sequence,
 - asynchronously passing elements between components,
 - with mandatory non-blocking backpressure.

The Reactive Streams specification consists of the following parts:

**The API** specifies the types to implement Reactive Streams and achieve interoperablility between different implementations.

***The Technology Compatibility Kit (TCK)*** is a standard test suite for conformance testing of implementations.

Implementations are free to implement additional features not covered by the specification as long as they conform to the API requirements and pass the tests in the TCK.

### API Components ###

The API consists of the following components that are required to be provided by Reactive Stream implementations:

 - Publisher
 - Subscriber
 - Subscription
 - Processor

A *Publisher* is a provider of a potentially unbounded number of sequenced elements, publishing them according to the demand received from its Subscriber(s). 

In response to a call to `Publisher.subscribe(Subscriber)` the possible invocation sequences for methods on the `Subscriber` are given by the following protocol:

```
onError | (onSubscribe onNext* (onError | onComplete)?)
```


NOTE: The specifications below use binding words in CAPLOCKS from https://www.ietf.org/rfc/rfc2119.txt

#### Subscriber ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Subscriber.java))

```java
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
````

- A `Subscriber` MUST NOT block a `Publisher` thread.
- A `Subscriber` MUST signal demand via `Subscription.request` to receive notifications.
- A `Subscriber` MAY behave synchronously or asynchronously but SHOULD NOT synchronously perform heavy computations in its methods (`onNext`, `onError`, `onComplete`, `onSubscribe`).


#### Publisher ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Publisher.java))

```java
public interface Publisher<T> {
    public void subscribe(Subscriber<T> s);
}
````

- The number of `onNext` events emitted by a `Publisher` to a `Subscriber` MUST NOT exceed the cumulative demand that has been signaled via that `Subscriber`’s `Subscription`.
- A `Publisher` MAY send less events than requested and terminate the `Subscription` by calling `onComplete` or `onError`.
- Events sent to a `Subscriber` MUST be sent sequentially (no concurrent notifications).
- If a `Publisher` fails it MUST emit an `onError`.
- If a `Publisher` terminates successfully (finite stream) it MUST emit an `onComplete`.
- If a Publisher signals either `onError` or `onComplete` on a `Subscriber`, that `Subscriber`’s `Subscription` MUST be considered canceled.
- Once a terminal state has been signaled (`onError`, `onComplete`) it is REQUIRED that no further events can be sent.
- Upon receiving a `Subscription.cancel` request it SHOULD stop sending events as soon as it can.
- `Subscription`'s which have been canceled SHOULD NOT receive subsequent `onError` or `onComplete` events, but implementations will not be able to strictly guarantee this in all cases due to the intrinsic race condition between actions taken concurrently by `Publisher` and `Subscriber`.
- A `Publisher` SHOULD NOT throw an `Exception`. The only legal way to signal failure (or reject a `Subscription`) is via the `Subscriber.onError` method.
- The `Subscriber.onSubscribe` method on a given `Subscriber` instance MUST NOT be called more than once.
- The `Publisher.subscribe` method MAY be called as many times as wanted but MUST be with a different `Subscriber` each time.
- A `Publisher` MAY support multi-subscribe and choose whether each `Subscription` is unicast or multicast.
- A `Publisher` MAY reject calls to its `subscribe` method if it is unable or unwilling to serve them (e.g. because it is overwhelmed or bounded by a finite number of underlying resources, etc...). If rejecting it MUST do this by calling `onError` on the `Subscriber` passed to `Publisher.subscribe` instead of calling `onSubscribe`".



#### Subscription ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Subscription.java))

```java
public interface Subscription {
    public void request(int n);
    public void cancel();
}
````

- A `Subscription` can be used once-and-only-once to represent a subscription by a `Subscriber` to a `Publisher`.
- Calls from a `Subscriber` to `Subscription.request(int n)` can be made directly since it is the responsibility of `Subscription` to handle async dispatching.
- The `Subscription.request` method MUST assume that it will be invoked synchronously and MUST NOT allow unbounded recursion such as `Subscriber.onNext` -> `Subscription.request` -> `Subscriber.onNext`. 
- The `Subscription.request` method SHOULD NOT synchronously perform heavy computations.
- The `Subscription.cancel` method MUST assume that it will be invoked synchronously and SHOULD NOT synchronously perform heavy computations.


#### Processor ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Processor.java))

```java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
````

- A Processor represents a processing stage—which is both a `Subscriber` and a `Publisher` and obeys the contracts of both.

### Asynchronous vs Synchronous Processing ###

The Reactive Streams API prescribes that all processing of elements (`onNext`) or termination signals (`onError`, `onComplete`) MUST NOT *block* the `Publisher`. However, each of the `on*` handlers can process the events synchronously or asynchronously. 

Take this example:

```
nioSelectorThreadOrigin map(f) filter(p) consumeTo(toNioSelectorOutput)
```

It has an async origin and an async destination. Let's assume that both origin and destination are selector event loops. The `Subscription.request(n)` must be chained from the destination to the origin. This is now where each implementation can choose how do do this.

The following uses the pipe `|` character to signal async boundaries (queue and schedule) and `R#` to represent resources (possibly threads).

```
nioSelectorThreadOrigin | map(f) | filter(p) | consumeTo(toNioSelectorOutput)
-------------- R1 ----  | - R2 - | -- R3 --- | ---------- R4 ----------------
```

In this example each of the 3 consumers, `map`, `filter` and `consumeTo` asynchronously schedule the work. It could be on the same event loop (trampoline), separate threads, whatever.

```
nioSelectorThreadOrigin map(f) filter(p) | consumeTo(toNioSelectorOutput)
------------------- R1 ----------------- | ---------- R2 ----------------
```

Here it is only the final step that asynchronously schedules, by adding work to the NioSelectorOutput event loop. The `map` and `filter` steps are synchronously performed on the origin thread.

Or another implementation could fuse the operations to the final consumer:

```
nioSelectorThreadOrigin | map(f) filter(p) consumeTo(toNioSelectorOutput)
--------- R1 ---------- | ------------------ R2 -------------------------
```

All of these variants are "asynchronous streams". They all have their place and each has different tradeoffs including performance and implementation complexity.

The Reactive Streams contract allows implementations the flexibility to manage resources and scheduling and mix asynchronous and synchronous processing within the bounds of a non-blocking, asynchronous, push-based stream.



### Subscriber controlled queue bounds ###

One of the underlying design principles is that all buffer sizes are to be bounded and these bounds must be *known* and *controlled* by the subscribers. These bounds are expressed in terms of *element count* (which in turn translates to the invocation count of onNext). Any implementation that aims to support infinite streams (especially high output rate streams) needs to enforce bounds all along the way to avoid out-of-memory errors and constrain resource usage in general.

Since back-pressure is mandatory the use of unbounded buffers can be avoided. In general, the only time when a queue might grow without bounds is when the publisher side maintains a higher rate than the subscriber for an extended period of time, but this scenario is handled by backpressure instead.

Queue bounds can be controlled by a subscriber by signaling demand for the appropriate number of elements. At any point in time the subscriber knows:

 - the total number of elements requested: `P`
 - the number of elements that have been processed: `N`

Then the maximum number of elements that may arrive—until more demand is signaled to the Publisher—is `P - N`. In the case that the subscriber also knows the number of elements B in its input buffer then this bound can be refined to `P - B - N`.

These bounds must be respected by a publisher independent of whether the source it represents can be backpressured or not. In the case of sources whose production rate cannot be influenced—for example clock ticks or mouse movement—the publisher must choose to either buffer or drop elements to obey the imposed bounds.

Subscribers signaling a demand for one element after the reception of an element effectively implement a Stop-and-Wait protocol where the demand signal is equivalent to acknowledgement. By providing demand for multiple elements the cost of acknowledgement is amortized. It is worth noting that the subscriber is allowed to signal demand at any point in time, allowing it to avoid unnecessary delays between the publisher and the subscriber (i.e. keeping its input buffer filled without having to wait for full round-trips).

## Legal

This project is a collaboration between engineers from Netflix, Twitter, RedHat, Pivotal, Typesafe and many others. The code is offered to the Public Domain in order to allow free use by interested parties who want to create compatible implementations. For details see `COPYING`.
