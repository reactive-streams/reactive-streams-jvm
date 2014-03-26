# Reactive Streams #

The purpose of Reactive Streams is to provide a standard for asynchronous stream processing with non-blocking backpressure.

## Goals, Design and Scope ##

Handling streams of data—especially “live” data whose volume is not predetermined—requires special care in an asynchronous system. The most prominent issue is that resource consumption needs to be carefully controlled such that a fast data source does not overwhelm the stream destination. Asynchrony is needed in order to enable the parallel use of computing resources, on collaborating network hosts or multiple CPU cores within a single machine.

The main goal of Reactive Streams is to govern the exchange of stream data across an asynchronous boundary—think passing  elements on to another thread or thread-pool—while ensuring that the receiving side is not forced to buffer arbitrary amounts of data. In other words, backpressure is an integral part of this model in order to allow the queues which mediate between threads to be bounded. The benefits of asynchronous processing would be negated if the communication of backpressure were synchronous (see also the [Reactive Manifesto](http://reactivemanifesto.org/)), therefore care has been taken to mandate fully non-blocking and asynchronous behavior of all aspects of a Reactive Streams implementation.

It is the intention of this specification to allow the creation of many conforming implementations, which by virtue of abiding by the rules will be able to interoperate smoothly, preserving the aforementioned benefits and characteristics across the whole processing graph of a stream application.

It should be noted that the precise nature of stream manipulations (transformation, splitting, merging, etc.) is not covered by this specification. Reactive Streams are only concerned with mediating the stream of data between different processing elements. In their development care has been taken to ensure that all basic ways of combining streams can be expressed.

In summary, Reactive Streams is a standard and specification for Stream-oriented libraries for the JVM that

 - process a potentially unbounded number of elements 
 - in sequence,
 - asynchronously passing elements between components,
 - with mandatory non-blocking backpressure.

The Reactive Streams specification consists of the following parts:

**The SPI** defines the interoperablility layer between different implementations.

**The API** specifies the types that the users of Reactive Stream libraries use.

***The Technology Compatibility Kit (TCK)*** is a standard test suite for conformance testing of implementations.

Implementations are free to implement additional features not covered by the specification as long as they conform to the API and SPI requirements and pass the tests in the TCK.

#### Comparison with related technologies ####

In contrast to reactive streams described in this document, a Future represents exactly one element (or a failure) that is produced asynchronosly while streams can provide a potentially unbounded number of elements.

Compared to Rx, the SPI described here prescribes a mandatory, non-blocking way to handle back-pressure and requires the processing of an element by a dowstream component to be dispatched asynchronously.

Iteratees are an abstraction used for consuming a stream, often for parsing it. In this sense they are not a stream transformation or combination tool in themselves.

### SPI Components ###

The SPI consists of components that are required to be provided by Reactive Stream implementations but these interfaces should not be exposed to libraries or user code that *use* a Reactive Streams implementation. The reason for this is that the methods used on the SPI level have very strict and rather complex semantic requirements which are likely to be violated by end users.

The components of the SPI are:

 - Publisher
 - Subscriber
 - Subscription

A *Publisher* is a provider of a potentially unbounded number of sequenced elements, publishing them according to the demand received from its Subscriber(s). A Publisher can serve multiple subscribers subscribed dynamically at various points in time. In the case of multiple subscribers the Publisher should respect the processing rates of all of its subscribers (possibly allowing for a bounded drift between them). It must eventually clean up its resources after all of its subscribers have been unsubscribed and shut down. A Publisher will typically support fanning out to multiple Subscribers in order to support the dynamic assembly of processing networks from building blocks that can freely be shared.

A *Subscriber* is a component that accepts a sequenced stream of elements provided by a Publisher. At any given time a Subscriber might be subscribed to at most one Publisher. It provides the callback onNext to be called by the upstream Producer, accepting an element that is to be asynchronously processed or enqueued without blocking the Producer. 

A Subscriber communicates demand to the Publisher via a *Subscription* which is passed to the Subscriber after the subscription has been established. The Subscription exposes the requestMore(int) method that is used by the Subscriber to signal demand to the Publisher. For each of its subscribers the Publisher obeys the following invariant:

*If N is the total number of demand tokens handed to the Publisher P by a Subscriber S during the time period up to a time T, then the number of onNext calls that are allowed to be performed by P on S before T must be less than or equal to N. The number of pending demand tokens must be tracked by the Producer separately for each of its subscribers.*

Subscribers that do not currently have an active subscription may subscribe to a Publisher. The only guarantee for subscribers attached at different points in time is that they all observe a common suffix of the stream, i.e. they receive the same elements after a certain point in time but it is not guaranteed that they see exactly the same number of elements. This obviously only holds if the subscriber does not cancel its subscription before the stream has been terminated.

> In practice there is a difference between the guarantees that different publishers can provide for subscribers attached at different points in time. For example Publishers serving elements from a strict collection (“cold”) might guarantee that all subscribers see *exactly* the same elements (unless unsubscribed before completion) since they can replay the elements from the collection at any point in time. Other publishers might represent an ephemeral source of elements (e.g. a “hot” TCP stream) and keep only a limited output buffer to replay for future subscribers.

At any time the Publisher may signal that it is not able to provide more elements. This is done by invoking onComplete on its subscribers.

> For example a Publisher representing a strict collection signals completion to its subscriber after it provided all the elements. Now a later subscriber might still receive the whole collection before receiving onComplete.

### API components ###

The purpose of the API is to provide the types that users interact with directly. SPI methods and interfaces should not be exposed expect for the purpose of writing Reactive Streams implementations.

The API counterpart for Publisher is *Producer* and for Subscriber is *Consumer*. The combination of these two—a stream processing element with asynchronous input and output—is called *Processor*.

### Asynchronous processing ###

The Reactive Streams SPI prescribes that all processing of elements (onNext) or termination signals (onError, onComplete) happens outside of the execution stack of the Publisher. This is achieved by scheduling the processing to run asynchronously, possibly on a different thread. The Subscriber should make sure to minimize the amount of processing steps used to initiate this process, meaning that all its SPI-mandated methods shall return as quickly as possible.

In contrast to communicating back-pressure by blocking the publisher, a non-blocking solution needs to communicate demand through a dedicated control channel. This channel is provided by the Subscription: the subscriber controls the maximum amount of future elements it is willing receive by sending explicit demand tokens (by calling requestMore(int)).

#### Relationship to synchronous stream-processing ####

This document describes asynchronous, non-blocking backpressure boundaries but in between those boundaries any kind of synchronous stream processing model is permitted. This is useful for performance optimization (eliminating inter-thread synchronization) and it conveniently transports backpressure implicitly (the calling method cannot continue while the call lasts). As an example consider a section consisting of three connected Processors, A, B and C:

    (...) --> A[S1 --> S2] --> B[S3 --> S4 --> S5] --> C[S6] --> (...)

Processor B is implemented in terms of three synchronous steps S3, S4 and S5. When communicating with its upstream Producer A, or its downstream Subscriber C it obeys the asynchronous, back-pressure aware requirements of the SPI, but internally it drives the synchronous stream of S3, S4, S5. 

> Please note that processing usually happens pipelined between A, B and C: assuming a stream of elements (E1, E2, E3) A might start processing E2 while C still processes E1. On the other hand inside A execution can be completely synchronous, so E3 might be only processed by S1 until E2 has left S2.

### Subscriber controlled queue bounds ###

One of the underlying design principles is that all buffer sizes are to be bounded and these bounds must be *known* and *controlled* by the subscribers. These bounds are expressed in terms of *element count* (which in turn translates to the invocation count of onNext). Any implementation that aims to support infinite streams (especially high output rate streams) needs to enforce bounds all along the way to avoid out-of-memory errors and constrain resource usage in general.

Since back-pressure is mandatory the use of unbounded buffers can be avoided. In general, the only time when a queue might grow without bounds is when the publisher side maintains a higher rate than the subscriber for an extended period of time, but this scenario is handled by backpressure instead.

Queue bounds can be controlled by a subscriber by signaling demand for the appropriate number of elements. At any point in time the subscriber knows:

 - the total number of elements requested: `P`
 - the number of elements that have been processed: `N`

Then the maximum number of elements that may arrive—until more demand is signaled to the Publisher—is `P - N`. In the case that the subscriber also knows the number of elements B in its input buffer then this bound can be refined to `P - B - N`.

These bounds must be respected by a publisher independent of whether the source it represents can be backpressured or not. In the case of sources whose production rate cannot be influenced—for example clock ticks or mouse movement—the publisher must choose to either buffer or drop elements to obey the imposed bounds.

Subscribers signaling a demand for one element after the reception of an element effectively implement a Stop-and-Wait protocol where the demand signal is equivalent to acknowledgement. By providing demand for multiple elements the cost of acknowledgement is amortized. It is worth noting that the subscriber is allowed to signal demand at any point in time, allowing it to avoid unnecessary delays between the publisher and the subscriber (i.e. keeping its input buffer filled without having to wait for full round-trips).

> Systems that use a signal to notify the publisher to suspend publishing cannot guarantee bounded queues. Since there is a delay between the time at which the signal has been raised and when it is processed, there is a window of time during which an arbitrary number of elements can be passed to the subscriber.

## Legal

This project is a collaboration between Netflix, Twitter, RedHat, Pivotal and Typesafe. The code is offered to the Public Domain in order to allow free use by interested parties who want to create compatible implementations. For details see `COPYING`.
