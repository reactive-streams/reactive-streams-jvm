# Reactive Streams #

The purpose of Reactive Streams is to provide a standard for asynchronous stream processing with non-blocking backpressure.

The latest preview release is available on Maven Central as

    <dependency>
      <groupId>org.reactivestreams</groupId>
      <artifactId>reactive-streams</artifactId>
      <version>0.4.0.M1</version>
    </dependency>
    <dependency>
      <groupId>org.reactivestreams</groupId>
      <artifactId>reactive-streams-tck</artifactId>
      <version>0.4.0.M1</version>
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

1. Publisher
2. Subscriber 
3. Subscription
4. Processor

A *Publisher* is a provider of a potentially unbounded number of sequenced elements, publishing them according to the demand received from its Subscriber(s). 

In response to a call to `Publisher.subscribe(Subscriber)` the possible invocation sequences for methods on the `Subscriber` are given by the following protocol:

```
onError | (onSubscribe onNext* (onError | onComplete)?)
```

#### NOTES

- The specifications below use binding words in capital letters from https://www.ietf.org/rfc/rfc2119.txt
- The terms emit, signal or send are interchangeable. The specifications below will use `signal`.
- The terms `synchronously` or `synchronous` refer to executing in the calling `Thread`.

### SPECIFICATION

#### 1. Publisher ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Publisher.java))

```java
public interface Publisher<T> {
    public void subscribe(Subscriber<T> s);
}
````

| ID     | Rule                                                                                                   |
| ------ | ------------------------------------------------------------------------------------------------------ |
| 1      | The number of `onNext` signaled by a `Publisher` to a `Subscriber` MUST NOT exceed the cumulative demand that has been signaled via that `Subscriber`’s `Subscription` |
| 2      | A `Publisher` MAY signal less `onNext` than requested and terminate the `Subscription` by calling `onComplete` or `onError` |
| 3      | `onSubscribe`, `onNext`, `onError` and `onComplete` signaled to a `Subscriber` MUST be signaled sequentially (no concurrent notifications) |
| 4      | If a `Publisher` fails it MUST signal an `onError` |
| 5      | If a `Publisher` terminates successfully (finite stream) it MUST signal an `onComplete` |
| 6      | If a `Publisher` signals either `onError` or `onComplete` on a `Subscriber`, that `Subscriber`’s `Subscription` MUST be considered canceled |
| 7      | Once a terminal state has been signaled (`onError`, `onComplete`) it is REQUIRED that no further signals occur. Situational scenario MAY apply [see 2.13] |
| 9      | `Subscription`'s which have been canceled SHOULD NOT receive subsequent `onError` or `onComplete` signals, but implementations will not be able to strictly guarantee this in all cases due to the intrinsic race condition between actions taken concurrently by `Publisher` and `Subscriber` |
| 10     | `Publisher.subscribe` SHOULD NOT throw a non-fatal  `Throwable`. The only legal way to signal failure (or reject a `Subscription`) is via the `onError` method. Non-fatal `Throwable` excludes any non-recoverable exception by the application (e.g. OutOfMemory) |
| 11     | `Publisher.subscribe` MAY be called as many times as wanted but MUST be with a different `Subscriber` each time [see 2.12]. It is RECOMMENDED to reject the `Subscription` with a `java.lang.IllegalStateException` if the same `Subscriber` already has an active `Subscription` with this `Publisher`. The cause message MUST include a reference to this rule and/or quote the full rule |
| 12     | A `Publisher` MAY support multi-subscribe and choose whether each `Subscription` is unicast or multicast |
| 13     | A `Publisher` MAY reject calls to its `subscribe` method if it is unable or unwilling to serve them [1]. If rejecting it MUST do this by calling `onError` on the `Subscriber` passed to `Publisher.subscribe` instead of calling `onSubscribe`" |
| 14     | A `Publisher` MUST produce the same elements, starting with the oldest element still available, in the same sequence for all its subscribers and MAY produce the stream elements at (temporarily) differing rates to different subscribers |

[1] :  A stateful Publisher can be overwhelmed, bounded by a finite number of underlying resources, exhausted, shut-down or in a failed state. 

#### 2. Subscriber ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Subscriber.java))

```java
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
````

| ID     | Rule                                                                                                   |
| ------ | ------------------------------------------------------------------------------------------------------ |
| 1      | A `Subscriber` MUST signal demand via `Subscription.request(long n)` to receive `onNext` signals |
| 2      | If a `Subscriber` suspects that its processing of signals will negatively impact its `Publisher`'s responsivity, it is RECOMMENDED that it asynchronously dispatches its signals |
| 3      | `Subscriber.onComplete()` and `Subscriber.onError(Throwable t)` MUST NOT call any methods on the `Subscription` or the `Publisher` |
| 4      | `Subscriber.onComplete()` and `Subscriber.onError(Throwable t)` MUST consider the Subscription cancelled after having received the signal |
| 5      | A `Subscriber` MUST call `Subscription.cancel()` on the given `Subscription` after an `onSubscribe` signal if it already has an active `Subscription` |
| 6      | A `Subscriber` MUST call `Subscription.cancel()` if it is no longer valid to the `Publisher` without the `Publisher` having signaled `onError` or `onComplete` |
| 7      | A `Subscriber` MUST ensure that all calls on its `Subscription` take place from the same thread or provide for respective external synchronization |
| 8      | A `Subscriber` MUST be prepared to receive one or more `onNext` signals after having called `Subscription.cancel()` if there are still requested elements pending [see 3.12]. `Subscription.cancel()` does not guarantee to perform the underlying cleaning operations immediately |
| 9      | A `Subscriber` MUST be prepared to receive an `onComplete` signal with or without a preceding `Subscription.request(long n)` call |
| 10     | A `Subscriber` MUST be prepared to receive an `onError` signal with or without a preceding `Subscription.request(long n)` call |
| 11     | A `Subscriber` MUST make sure that all calls on its `onXXX` methods happen-before [1] the processing of the respective signals. I.e. the Subscriber must take care of properly publishing the signal to its processing logic |
| 12     | `Subscriber.onSubscribe` MUST NOT be called more than once (based on object equality) |
| 13     | A failing `onComplete` invocation (e.g. throwing an exception) is a specification violation and MUST signal `onError` with `java.lang.IllegalStateException`. The cause message MUST include a reference to this rule and/or quote the full rule |
| 14     | A failing `onError` invocation (e.g. throwing an exception) is a violation of the specification. In this case the `Publisher` MUST consider a possible `Subscription` for this `Subscriber` as canceled. The `Publisher` MUST raise this error condition in a fashion that is adequate for the runtime environment (e.g. by throwing an exception, notifying a supervisor, logging, etc.). |

[1] : See JMM definition of Happen-Before in section 17.4.5. on http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html

#### 3. Subscription ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Subscription.java))

```java
public interface Subscription {
    public void request(int n);
    public void cancel();
}
````

| ID     | Rule                                                                                                   |
| ------ | ------------------------------------------------------------------------------------------------------ |
| 1      | `Subscription.request` or `Subscription.cancel` MUST not be called outside of its `Subscriber` context. A `Subscription` represents the unique relationship between a `Subscriber` and a `Publisher` [see 2.12] |
| 2      | The `Subscription` MUST allow the `Subscriber` to call `Subscription.request` synchronously from within `onNext` or `onSubscribe` |
| 3      | `Subscription.request` MUST NOT allow unbounded recursion such as `Subscriber.onNext` -> `Subscription.request` -> `Subscriber.onNext` |
| 4      | `Subscription.request` SHOULD NOT synchronously perform heavy computations that would impact its caller's responsivity |
| 5      | `Subscription.cancel` MUST NOT synchronously perform heavy computations, MUST be idempotent and MUST be thread-safe |
| 6      | After the `Subscription` is cancelled, additional `Subscription.request(long n)` MUST be NOPs |
| 7      | After the `Subscription` is cancelled, additional `Subscription.cancel()` MUST be NOPs |
| 8      | While the `Subscription` is not cancelled, `Subscription.request(long n)` MUST register the given number of additional elements to be produced to the respective subscriber |
| 9      | While the `Subscription` is not cancelled, `Subscription.request(long n)` MUST throw a `java.lang.IllegalArgumentException` if the argument is <= 0. The cause message MUST include a reference to this rule and/or quote the full rule |
| 10     | While the `Subscription` is not cancelled, `Subscription.request(long n)` MAY synchronously call `onNext` on this (or other) subscriber(s) |
| 11     | While the `Subscription` is not cancelled, `Subscription.request(long n)` MAY synchronously call `onComplete` or `onError` on this (or other) subscriber(s) |
| 12     | While the `Subscription` is not cancelled, `Subscription.cancel()` MUST request the `Publisher` to eventually stop signaling its `Subscriber`. The operation is NOT REQUIRED to affect the `Subscription` immediately. |
| 13     | While the `Subscription` is not cancelled, `Subscription.cancel()` MUST request the `Publisher` to eventually drop any references to the corresponding subscriber. Re-subscribing with the same `Subscriber` object is discouraged [see 2.12], but this specification does not mandate that it is disallowed since that would mean having to store previously canceled subscriptions indefinitely |
| 14     | While the `Subscription` is not cancelled, invoking `Subscription.cancel` MAY cause the `Publisher` to transition into the `shut-down` state if no other `Subscription` exists at this point [see 1.17].
| 15     | `Subscription.cancel` MUST NOT throw an `Exception` and MUST signal `onError` to its `Subscriber` |
| 16     | `Subscription.request` MUST NOT throw an `Exception` and MUST signal `onError` to its `Subscriber` |
| 17     | A `Subscription MUST support an unbounded number of calls to request and MUST support a pending request count up to 2^63-1 (java.lang.Long.MAX_VALUE). A pending request count of exactly 2^63-1 (java.lang.Long.MAX_VALUE) MAY be considered by the `Publisher` as `effectively unbounded`[1]. If more than 2^63-1 are requested in pending then it MUST signal an onError with `java.lang.IllegalStateException` on the given `Subscriber`. The cause message MUST include a reference to this rule and/or quote the full rule. |

[1] : As it is not feasibly reachable with current or forseen hardware within a reasonable amount of time (1 element per nanosecond would take 292 years) to fulfill a demand of 2^63-1.

A `Subscription` is shared by exactly one `Publisher` and one `Subscriber` for the purpose of mediating the data exchange between this pair. This is the reason why the `subscribe()` method does not return the created `Subscription`, but instead returns `void`; the `Subscription` is only passed to the `Subscriber` via the `onSubscribe` callback.

#### 4.Processor ([Code](https://github.com/reactive-streams/reactive-streams/blob/master/api/src/main/java/org/reactivestreams/Processor.java))

```java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
````

| ID     | Rule                                                                                                   |
| ------ | ------------------------------------------------------------------------------------------------------ |
| 1      | A `Processor` represents a processing stage—which is both a `Subscriber` and a `Publisher` and MUST obey the contracts of both [1] |
| 2      | A `Processor` MAY choose to recover an `onError` signal. If it chooses to do so, it MUST consider the `Subscription` canceled, otherwise it MUST propagate the `onError` signal to its Subscribers immediately |

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

In order to allow fully asynchronous implementations of all participating API elements—`Publisher`/`Subscription`/`Subscriber`/`Processor`—all methods defined by these interfaces return `void`.


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

This project is a collaboration between engineers from Kaazing, Netflix, Pivotal, RedHat, Twitter, Typesafe and many others. The code is offered to the Public Domain in order to allow free use by interested parties who want to create compatible implementations. For details see `COPYING`.
