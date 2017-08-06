# Reactive Streams #

The purpose of Reactive Streams is to provide a standard for asynchronous stream processing with non-blocking backpressure.

The latest release is available on Maven Central as

```xml
<dependency>
  <groupId>org.reactivestreams</groupId>
  <artifactId>reactive-streams</artifactId>
  <version>1.0.1</version>
</dependency>
<dependency>
  <groupId>org.reactivestreams</groupId>
  <artifactId>reactive-streams-tck</artifactId>
  <version>1.0.1</version>
  <scope>test</scope>
</dependency>
```

## Goals, Design and Scope ##

Handling streams of data—especially “live” data whose volume is not predetermined—requires special care in an asynchronous system. The most prominent issue is that resource consumption needs to be carefully controlled such that a fast data source does not overwhelm the stream destination. Asynchrony is needed in order to enable the parallel use of computing resources, on collaborating network hosts or multiple CPU cores within a single machine.

The main goal of Reactive Streams is to govern the exchange of stream data across an asynchronous boundary – think passing elements on to another thread or thread-pool — while ensuring that the receiving side is not forced to buffer arbitrary amounts of data. In other words, backpressure is an integral part of this model in order to allow the queues which mediate between threads to be bounded. The benefits of asynchronous processing would be negated if the backpressure signals were synchronous (see also the [Reactive Manifesto](http://reactivemanifesto.org/)), therefore care has been taken to mandate fully non-blocking and asynchronous behavior of all aspects of a Reactive Streams implementation.

It is the intention of this specification to allow the creation of many conforming implementations, which by virtue of abiding by the rules will be able to interoperate smoothly, preserving the aforementioned benefits and characteristics across the whole processing graph of a stream application.

It should be noted that the precise nature of stream manipulations (transformation, splitting, merging, etc.) is not covered by this specification. Reactive Streams are only concerned with mediating the stream of data between different [API Components](#api-components). In their development care has been taken to ensure that all basic ways of combining streams can be expressed.

In summary, Reactive Streams is a standard and specification for Stream-oriented libraries for the JVM that

 - process a potentially unbounded number of elements
 - in sequence,
 - asynchronously passing elements between components,
 - with mandatory non-blocking backpressure.

The Reactive Streams specification consists of the following parts:

***The API*** specifies the types to implement Reactive Streams and achieve interoperability between different implementations.

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
onSubscribe onNext* (onError | onComplete)?
```

This means that `onSubscribe` is always signalled,
followed by a possibly unbounded number of `onNext` signals (as requested by `Subscriber`) followed by an `onError` signal if there is a failure, or an `onComplete` signal when no more elements are available—all as long as the `Subscription` is not cancelled.

#### NOTES

- The specifications below use binding words in capital letters from https://www.ietf.org/rfc/rfc2119.txt

### Glossary

| Term                      | Definition                                                                                             |
| ------------------------- | ------------------------------------------------------------------------------------------------------ |
| <a name="term_signal">Signal</a> | As a noun: one of the `onSubscribe`, `onNext`, `onComplete`, `onError`, `request(n)` or `cancel` methods. As a verb: calling/invoking a signal. |
| <a name="term_demand">Demand</a> | As a noun, the aggregated number of elements requested by a Subscriber which is yet to be delivered (fulfilled) by the Publisher. As a verb, the act of `request`-ing more elements. |
| <a name="term_sync">Synchronous(ly)</a> | Executes on the calling Thread. |
| <a name="term_return_normally">Return normally</a> | Only ever returns a value of the declared type to the caller. The only legal way to signal failure to a `Subscriber` is via the `onError` method.|
| <a name="term_responsivity">Responsivity</a> | Readiness/ability to respond. In this document used to indicate that the different components should not impair each others ability to respond. |
| <a name="term_non-obstructing">Non-obstructing</a> | Quality describing a method which is as quick to execute as possible—on the calling thread. This means, for example, avoids heavy computations and other things that would stall the caller´s thread of execution. |
| <a name="term_terminal_state">Terminal state</a> | For a Publisher: When `onComplete` or `onError` has been signalled. For a Subscriber: When an `onComplete` or `onError` has been received.|
| <a name="term_nop">NOP</a> | Execution that has no detectable effect to the calling thread, and can as such safely be called any number of times.|
| <a name="term_ext_sync">External synchronization</a> | Access coordination for thread safety purposes implemented outside of the constructs defined in this specification, using techniques such as, but not limited to, `atomics`, `monitors`, or `locks`. |

### SPECIFICATION

#### 1. Publisher ([Code](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.1/api/src/main/java/org/reactivestreams/Publisher.java))

```java
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
````

| ID                        | Rule                                                                                                   |
| ------------------------- | ------------------------------------------------------------------------------------------------------ |
| <a name="1.1">1</a>       | The total number of `onNext`´s signalled by a `Publisher` to a `Subscriber` MUST be less than or equal to the total number of elements requested by that `Subscriber`´s `Subscription` at all times. |
| [:bulb:](#1.1 "1.1 explained") | *The intent of this rule is to make it clear that Publishers cannot signal more elements than Subscribers have requested. There’s an implicit, but important, consequence to this rule: Since demand can only be fulfilled after it has been received, there’s a happens-before relationship between requesting elements and receiving elements.* |
| <a name="1.2">2</a>       | A `Publisher` MAY signal fewer `onNext` than requested and terminate the `Subscription` by calling `onComplete` or `onError`. |
| [:bulb:](#1.2 "1.2 explained") | *The intent of this rule is to make it clear that a Publisher cannot guarantee that it will be able to produce the number of elements requested; it simply might not be able to produce them all; it may be in a failed state; it may be empty or otherwise already completed.* |
| <a name="1.3">3</a>       | `onSubscribe`, `onNext`, `onError` and `onComplete` signaled to a `Subscriber` MUST be signaled in a `thread-safe` manner—and if performed by multiple threads—use [external synchronization](#term_ext_sync). |
| [:bulb:](#1.3 "1.3 explained") | *The intent of this rule is to make it clear that [external synchronization](#term_ext_sync) must be employed if the Publisher intends to send signals from multiple/different threads.* |
| <a name="1.4">4</a>       | If a `Publisher` fails it MUST signal an `onError`. |
| [:bulb:](#1.4 "1.4 explained") | *The intent of this rule is to make it clear that a Publisher is responsible for notifying its Subscribers if it detects that it cannot proceed—Subscribers must be given a chance to clean up resources or otherwise deal with the Publisher´s failures.* |
| <a name="1.5">5</a>       | If a `Publisher` terminates successfully (finite stream) it MUST signal an `onComplete`. |
| [:bulb:](#1.5 "1.5 explained") | *The intent of this rule is to make it clear that a Publisher is responsible for notifying its Subscribers that it has reached a [terminal state](#term_terminal_state)—Subscribers can then act on this information; clean up resources, etc.* |
| <a name="1.6">6</a>       | If a `Publisher` signals either `onError` or `onComplete` on a `Subscriber`, that `Subscriber`’s `Subscription` MUST be considered cancelled. |
| [:bulb:](#1.6 "1.6 explained") | *The intent of this rule is to make sure that a Subscription is treated the same no matter if it was cancelled, the Publisher signalled onError or onComplete.* |
| <a name="1.7">7</a>       | Once a [terminal state](#term_terminal_state) has been signaled (`onError`, `onComplete`) it is REQUIRED that no further signals occur. |
| [:bulb:](#1.7 "1.7 explained") | *The intent of this rule is to make sure that onError and onComplete are the final states of an interaction between a Publisher and Subscriber pair.* |
| <a name="1.8">8</a>       | If a `Subscription` is cancelled its `Subscriber` MUST eventually stop being signaled. |
| [:bulb:](#1.8 "1.8 explained") | *The intent of this rule is to make sure that Publishers respect a Subscriber’s request to cancel a Subscription when Subscription.cancel() has been called. The reason for *eventually* is because signals can have propagation delay due to being asynchronous.* |
| <a name="1.9">9</a>       | `Publisher.subscribe` MUST call `onSubscribe` on the provided `Subscriber` prior to any other signals to that `Subscriber` and MUST [return normally](#term_return_normally), except when the provided `Subscriber` is `null` in which case it MUST throw a `java.lang.NullPointerException` to the caller, for all other situations the only legal way to signal failure (or reject the `Subscriber`) is by calling `onError` (after calling `onSubscribe`). |
| [:bulb:](#1.9 "1.9 explained") | *The intent of this rule is to make sure that `onSubscribe` is always signalled before any of the other signals, so that initialization logic can be executed by the Subscriber when the signal is received. Also `onSubscribe` MUST only be called at most once, [see [2.12](#2.12)]. If the supplied `Subscriber` is `null`, there is nowhere else to signal this but to the caller, which means a `java.lang.NullPointerException` must be thrown. Examples of possible situations: A stateful Publisher can be overwhelmed, bounded by a finite number of underlying resources, exhausted, or in a [terminal state](#term_terminal_state).* |
| <a name="1.10">10</a>     | `Publisher.subscribe` MAY be called as many times as wanted but MUST be with a different `Subscriber` each time [see [2.12](#2.12)]. |
| [:bulb:](#1.10 "1.10 explained") | *The intent of this rule is to have callers of `subscribe` be aware that a generic Publisher and a generic Subscriber cannot be assumed to support being attached multiple times. Furthermore, it also mandates that the semantics of `subscribe` must be upheld no matter how many times it is called.* |
| <a name="1.11">11</a>     | A `Publisher` MAY support multiple `Subscriber`s and decides whether each `Subscription` is unicast or multicast. |
| [:bulb:](#1.11 "1.11 explained") | *The intent of this rule is to give Publisher implementations the flexibility to decide how many, if any, Subscribers they will support, and how elements are going to be distributed.* |

#### 2. Subscriber ([Code](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.1/api/src/main/java/org/reactivestreams/Subscriber.java))

```java
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
````

| ID                        | Rule                                                                                                   |
| ------------------------- | ------------------------------------------------------------------------------------------------------ |
| <a name="2.1">1</a>       | A `Subscriber` MUST signal demand via `Subscription.request(long n)` to receive `onNext` signals. |
| [:bulb:](#2.1 "2.1 explained") | *The intent of this rule is to establish that it is the responsibility of the Subscriber to signal when, and how many, elements it is able and willing to receive.* |
| <a name="2.2">2</a>       | If a `Subscriber` suspects that its processing of signals will negatively impact its `Publisher`´s responsivity, it is RECOMMENDED that it asynchronously dispatches its signals. |
| [:bulb:](#2.2 "2.2 explained") | *The intent of this rule is that a Subscriber should [not obstruct](#term_non-obstructing) the progress of the Publisher from an execution point-of-view. In other words, the Subscriber should not starve the Publisher from CPU cycles.* |
| <a name="2.3">3</a>       | `Subscriber.onComplete()` and `Subscriber.onError(Throwable t)` MUST NOT call any methods on the `Subscription` or the `Publisher`. |
| [:bulb:](#2.3 "2.3 explained") | *The intent of this rule is to prevent cycles and race-conditions—between Publisher, Subscription and Subscriber—during the processing of completion signals.* |
| <a name="2.4">4</a>       | `Subscriber.onComplete()` and `Subscriber.onError(Throwable t)` MUST consider the Subscription cancelled after having received the signal. |
| [:bulb:](#2.4 "2.4 explained") | *The intent of this rule is to make sure that Subscribers respect a Publisher’s [terminal state](#term_terminal_state) signals. A Subscription is simply not valid anymore after an onComplete or onError signal has been received.* |
| <a name="2.5">5</a>       | A `Subscriber` MUST call `Subscription.cancel()` on the given `Subscription` after an `onSubscribe` signal if it already has an active `Subscription`. |
| [:bulb:](#2.5 "2.5 explained") | *The intent of this rule is to prevent that two, or more, separate Publishers from thinking that they can interact with the same Subscriber. Enforcing this rule means that resource leaks are prevented since extra Subscriptions will be cancelled.* |
| <a name="2.6">6</a>       | A `Subscriber` MUST call `Subscription.cancel()` if the `Subscription` is no longer needed. |
| [:bulb:](#2.6 "2.6 explained") | *The intent of this rule is to establish that Subscribers cannot just throw Subscriptions away when they are no longer needed, they have to call `cancel` so that resources held by that Subscription can be safely, and timely, reclaimed. An example of this would be a Subscriber which is only interested in a specific element, which would then cancel its Subscription to signal its completion to the Publisher.* |
| <a name="2.7">7</a>       | A `Subscriber` MUST ensure that all calls on its `Subscription` take place from the same thread or provide for respective [external synchronization](#term_ext_sync). |
| [:bulb:](#2.7 "2.7 explained") | *The intent of this rule is to establish that [external synchronization](#term_ext_sync) must be added if a Subscriber will be using a Subscription concurrently by two or more threads.* |
| <a name="2.8">8</a>       | A `Subscriber` MUST be prepared to receive one or more `onNext` signals after having called `Subscription.cancel()` if there are still requested elements pending [see [3.12](#3.12)]. `Subscription.cancel()` does not guarantee to perform the underlying cleaning operations immediately. |
| [:bulb:](#2.8 "2.8 explained") | *The intent of this rule is to highlight that there may be a delay between calling `cancel` the Publisher seeing that.* |
| <a name="2.9">9</a>       | A `Subscriber` MUST be prepared to receive an `onComplete` signal with or without a preceding `Subscription.request(long n)` call. |
| [:bulb:](#2.9 "2.9 explained") | *The intent of this rule is to establish that completion is unrelated to the demand flow—this allows for streams which complete early, and obviates the need to *poll* for completion.* |
| <a name="2.10">10</a>     | A `Subscriber` MUST be prepared to receive an `onError` signal with or without a preceding `Subscription.request(long n)` call. |
| [:bulb:](#2.10 "2.10 explained") | *The intent of this rule is to establish that Publisher failures may be completely unrelated to signalled demand. This means that Subscribers do not need to poll to find out if the Publisher will not be able to fulfill its requests.* |
| <a name="2.11">11</a>     | A `Subscriber` MUST make sure that all calls on its [signal](#term_signal) methods happen-before the processing of the respective signals. I.e. the Subscriber must take care of properly publishing the signal to its processing logic. |
| [:bulb:](#2.11 "2.11 explained") | *The intent of this rule is to establish that it is the responsibility of the Subscriber implementation to make sure that asynchronous processing of its signals are thread safe. See [JMM definition of Happens-Before in section 17.4.5](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4.5).* |
| <a name="2.12">12</a>     | `Subscriber.onSubscribe` MUST be called at most once for a given `Subscriber` (based on object equality). |
| [:bulb:](#2.12 "2.12 explained") | *The intent of this rule is to establish that it MUST be assumed that the same Subscriber can only be subscribed at most once. Note that `object equality` is `a.equals(b)`.* |
| <a name="2.13">13</a>     | Calling `onSubscribe`, `onNext`, `onError` or `onComplete` MUST [return normally](#term_return_normally) except when any provided parameter is `null` in which case it MUST throw a `java.lang.NullPointerException` to the caller, for all other situations the only legal way for a `Subscriber` to signal failure is by cancelling its `Subscription`. In the case that this rule is violated, any associated `Subscription` to the `Subscriber` MUST be considered as cancelled, and the caller MUST raise this error condition in a fashion that is adequate for the runtime environment. |
| [:bulb:](#2.13 "2.13 explained") | *The intent of this rule is to establish the semantics for the methods of Subscriber and what the Publisher is allowed to do in which case this rule is violated. «Raise this error condition in a fashion that is adequate for the runtime environment» could mean logging the error—or otherwise make someone or something aware of the situation—as the error cannot be signalled to the faulty Subscriber.* |

#### 3. Subscription ([Code](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.1/api/src/main/java/org/reactivestreams/Subscription.java))

```java
public interface Subscription {
    public void request(long n);
    public void cancel();
}
````

| ID                        | Rule                                                                                                   |
| ------------------------- | ------------------------------------------------------------------------------------------------------ |
| <a name="3.1">1</a>       | `Subscription.request` and `Subscription.cancel` MUST only be called inside of its `Subscriber` context. |
| [:bulb:](#3.1 "3.1 explained") | *The intent of this rule is to establish that a Subscription represents the unique relationship between a Subscriber and a Publisher [see [2.12](#2.12)]. The Subscriber is in control over when elements are requested and when more elements are no longer needed.* |
| <a name="3.2">2</a>       | The `Subscription` MUST allow the `Subscriber` to call `Subscription.request` synchronously from within `onNext` or `onSubscribe`. |
| [:bulb:](#3.2 "3.2 explained") | *The intent of this rule is to make it clear that implementations of `request` must be reentrant, to avoid stack overflows in the case of mutual recursion between `request` and `onNext` (and eventually `onComplete` / `onError`). This implies that Publishers can be `synchronous`, i.e. signalling `onNext`´s on the thread which calls `request`.* |
| <a name="3.3">3</a>       | `Subscription.request` MUST place an upper bound on possible synchronous recursion between `Publisher` and `Subscriber`. |
| [:bulb:](#3.3 "3.3 explained") | *The intent of this rule is to complement [see [3.2](#3.2)] by placing an upper limit on the mutual recursion between `request` and `onNext` (and eventually `onComplete` / `onError`). Implementations are RECOMMENDED to limit this mutual recursion to a depth of `1` (ONE)—for the sake of conserving stack space. An example for undesirable synchronous, open recursion would be Subscriber.onNext -> Subscription.request -> Subscriber.onNext -> …, as it otherwise will result in blowing the calling Thread´s stack.* |
| <a name="3.4">4</a>       | `Subscription.request` SHOULD respect the responsivity of its caller by returning in a timely manner. |
| [:bulb:](#3.4 "3.4 explained") | *The intent of this rule is to establish that `request` is intended to be a [non-obstructing](#term_non-obstructing) method, and should be as quick to execute as possible on the calling thread, so avoid heavy computations and other things that would stall the caller´s thread of execution.* |
| <a name="3.5">5</a>       | `Subscription.cancel` MUST respect the responsivity of its caller by returning in a timely manner, MUST be idempotent and MUST be thread-safe. |
| [:bulb:](#3.5 "3.5 explained") | *The intent of this rule is to establish that `cancel` is intended to be a [non-obstructing](#term_non-obstructing) method, and should be as quick to execute as possible on the calling thread, so avoid heavy computations and other things that would stall the caller´s thread of execution. Furthermore, it is also important that it is possible to call it multiple times without any adverse effects.* |
| <a name="3.6">6</a>       | After the `Subscription` is cancelled, additional `Subscription.request(long n)` MUST be [NOPs](#term_nop). |
| [:bulb:](#3.6 "3.6 explained") | *The intent of this rule is to establish a causal relationship between cancellation of a subscription and the subsequent non-operation of requesting more elements.* |
| <a name="3.7">7</a>       | After the `Subscription` is cancelled, additional `Subscription.cancel()` MUST be [NOPs](#term_nop). |
| [:bulb:](#3.7 "3.7 explained") | *The intent of this rule is superseded by [3.5](#3.5).* |
| <a name="3.8">8</a>       | While the `Subscription` is not cancelled, `Subscription.request(long n)` MUST register the given number of additional elements to be produced to the respective subscriber. |
| [:bulb:](#3.8 "3.8 explained") | *The intent of this rule is to make sure that `request`-ing is an additive operation, as well as ensuring that a request for elements is delivered to the Publisher.* |
| <a name="3.9">9</a>       | While the `Subscription` is not cancelled, `Subscription.request(long n)` MUST signal `onError` with a `java.lang.IllegalArgumentException` if the argument is <= 0. The cause message SHOULD explain that non-positive request signals are illegal. |
| [:bulb:](#3.9 "3.9 explained") | *The intent of this rule is to prevent faulty implementations to proceed operation without any exceptions being raised. Requesting a negative or 0 number of elements, since requests are additive, most likely to be the result of an erroneous calculation on the behalf of the Subscriber.* |
| <a name="3.10">10</a>     | While the `Subscription` is not cancelled, `Subscription.request(long n)` MAY synchronously call `onNext` on this (or other) subscriber(s). |
| [:bulb:](#3.10 "3.10 explained") | *The intent of this rule is to establish that it is allowed to create synchronous Publishers, i.e. Publishers who execute their logic on the calling thread.* |
| <a name="3.11">11</a>     | While the `Subscription` is not cancelled, `Subscription.request(long n)` MAY synchronously call `onComplete` or `onError` on this (or other) subscriber(s). |
| [:bulb:](#3.11 "3.11 explained") | *The intent of this rule is to establish that it is allowed to create synchronous Publishers, i.e. Publishers who execute their logic on the calling thread.* |
| <a name="3.12">12</a>     | While the `Subscription` is not cancelled, `Subscription.cancel()` MUST request the `Publisher` to eventually stop signaling its `Subscriber`. The operation is NOT REQUIRED to affect the `Subscription` immediately. |
| [:bulb:](#3.12 "3.12 explained") | *The intent of this rule is to establish that the desire to cancel a Subscription is eventually respected by the Publisher, acknowledging that it may take some time before the signal is received.* |
| <a name="3.13">13</a>     | While the `Subscription` is not cancelled, `Subscription.cancel()` MUST request the `Publisher` to eventually drop any references to the corresponding subscriber. |
| [:bulb:](#3.13 "3.13 explained") | *The intent of this rule is to make sure that Subscribers can be properly garbage-collected after their subscription no longer being valid. Re-subscribing with the same Subscriber object is discouraged [see [2.12](#2.12)], but this specification does not mandate that it is disallowed since that would mean having to store previously cancelled subscriptions indefinitely.* |
| <a name="3.14">14</a>     | While the `Subscription` is not cancelled, calling `Subscription.cancel` MAY cause the `Publisher`, if stateful, to transition into the `shut-down` state if no other `Subscription` exists at this point [see [1.9](#1.9)]. |
| [:bulb:](#3.14 "3.14 explained") | *The intent of this rule is to allow for Publishers to signal `onComplete` or `onError` following `onSubscribe` for new Subscribers in response to a cancellation signal from an existing Subscriber.* |
| <a name="3.15">15</a>     | Calling `Subscription.cancel` MUST [return normally](#term_return_normally). |
| [:bulb:](#3.15 "3.15 explained") | *The intent of this rule is to disallow implementations to throw exceptions in response to `cancel` being called.* |
| <a name="3.16">16</a>     | Calling `Subscription.request` MUST [return normally](#term_return_normally). |
| [:bulb:](#3.16 "3.16 explained") | *The intent of this rule is to disallow implementations to throw exceptions in response to `request` being called.* |
| <a name="3.17">17</a>     | A `Subscription` MUST support an unbounded number of calls to `request` and MUST support a demand up to 2^63-1 (`java.lang.Long.MAX_VALUE`). A demand equal or greater than 2^63-1 (`java.lang.Long.MAX_VALUE`) MAY be considered by the `Publisher` as “effectively unbounded”. |
| [:bulb:](#3.17 "3.17 explained") | *The intent of this rule is to establish that the Subscriber can request an unbounded number of elements, in any increment above 0 [see [3.9](#3.9)], in any number of invocations of `request`. As it is not feasibly reachable with current or foreseen hardware within a reasonable amount of time (1 element per nanosecond would take 292 years) to fulfill a demand of 2^63-1, it is allowed for a Publisher to stop tracking demand beyond this point.* |

A `Subscription` is shared by exactly one `Publisher` and one `Subscriber` for the purpose of mediating the data exchange between this pair. This is the reason why the `subscribe()` method does not return the created `Subscription`, but instead returns `void`; the `Subscription` is only passed to the `Subscriber` via the `onSubscribe` callback.

#### 4.Processor ([Code](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.1/api/src/main/java/org/reactivestreams/Processor.java))

```java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
````

| ID                       | Rule                                                                                                   |
| ------------------------ | ------------------------------------------------------------------------------------------------------ |
| <a name="4.1">1</a>      | A `Processor` represents a processing stage—which is both a `Subscriber` and a `Publisher` and MUST obey the contracts of both. |
| [:bulb:](#4.1 "4.1 explained") | *The intent of this rule is to establish that Processors behave, and are bound by, both the Publisher and Subscriber specifications.* |
| <a name="4.2">2</a>      | A `Processor` MAY choose to recover an `onError` signal. If it chooses to do so, it MUST consider the `Subscription` cancelled, otherwise it MUST propagate the `onError` signal to its Subscribers immediately. |
| [:bulb:](#4.2 "4.2 explained") | *The intent of this rule is to inform that it’s possible for implementations to be more than simple transformations.* |

While not mandated, it can be a good idea to cancel a `Processors` upstream `Subscription` when/if its last `Subscriber` cancels their `Subscription`,
to let the cancellation signal propagate upstream.

### Asynchronous vs Synchronous Processing ###

The Reactive Streams API prescribes that all processing of elements (`onNext`) or termination signals (`onError`, `onComplete`) MUST NOT *block* the `Publisher`. However, each of the `on*` handlers can process the events synchronously or asynchronously.

Take this example:

```
nioSelectorThreadOrigin map(f) filter(p) consumeTo(toNioSelectorOutput)
```

It has an async origin and an async destination. Let’s assume that both origin and destination are selector event loops. The `Subscription.request(n)` must be chained from the destination to the origin. This is now where each implementation can choose how to do this.

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

The Reactive Streams contract allows implementations the flexibility to manage resources and scheduling and mix asynchronous and synchronous processing within the bounds of a non-blocking, asynchronous, dynamic push-pull stream.

In order to allow fully asynchronous implementations of all participating API elements—`Publisher`/`Subscription`/`Subscriber`/`Processor`—all methods defined by these interfaces return `void`.

### Subscriber controlled queue bounds ###

One of the underlying design principles is that all buffer sizes are to be bounded and these bounds must be *known* and *controlled* by the subscribers. These bounds are expressed in terms of *element count* (which in turn translates to the invocation count of onNext). Any implementation that aims to support infinite streams (especially high output rate streams) needs to enforce bounds all along the way to avoid out-of-memory errors and constrain resource usage in general.

Since back-pressure is mandatory the use of unbounded buffers can be avoided. In general, the only time when a queue might grow without bounds is when the publisher side maintains a higher rate than the subscriber for an extended period of time, but this scenario is handled by backpressure instead.

Queue bounds can be controlled by a subscriber signaling demand for the appropriate number of elements. At any point in time the subscriber knows:

 - the total number of elements requested: `P`
 - the number of elements that have been processed: `N`

Then the maximum number of elements that may arrive—until more demand is signaled to the Publisher—is `P - N`. In the case that the subscriber also knows the number of elements B in its input buffer then this bound can be refined to `P - B - N`.

These bounds must be respected by a publisher independent of whether the source it represents can be backpressured or not. In the case of sources whose production rate cannot be influenced—for example clock ticks or mouse movement—the publisher must choose to either buffer or drop elements to obey the imposed bounds.

Subscribers signaling a demand for one element after the reception of an element effectively implement a Stop-and-Wait protocol where the demand signal is equivalent to acknowledgement. By providing demand for multiple elements the cost of acknowledgement is amortized. It is worth noting that the subscriber is allowed to signal demand at any point in time, allowing it to avoid unnecessary delays between the publisher and the subscriber (i.e. keeping its input buffer filled without having to wait for full round-trips).

## Legal

This project is a collaboration between engineers from Kaazing, Lightbend, Netflix, Pivotal, Red Hat, Twitter and many others. The code is offered to the Public Domain in order to allow free use by interested parties who want to create compatible implementations. For details see `COPYING`.

<p xmlns:dct="http://purl.org/dc/terms/" xmlns:vcard="http://www.w3.org/2001/vcard-rdf/3.0#">
  <a rel="license" href="http://creativecommons.org/publicdomain/zero/1.0/">
    <img src="http://i.creativecommons.org/p/zero/1.0/88x31.png" style="border-style: none;" alt="CC0" />
  </a>
  <br />
  To the extent possible under law,
  <a rel="dct:publisher" href="http://www.reactive-streams.org/">
    <span property="dct:title">Reactive Streams Special Interest Group</span></a>
  has waived all copyright and related or neighboring rights to
  <span property="dct:title">Reactive Streams JVM</span>.
  This work is published from:
  <span property="vcard:Country" datatype="dct:ISO3166" content="US" about="http://www.reactive-streams.org/">United States</span>.
</p>

