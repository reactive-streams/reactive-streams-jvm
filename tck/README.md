# Reactive Streams TCK #

The purpose of the *Reactive Streams Technology Compatibility Kit* (from here on referred to as: *the TCK*) is to guide
and help Reactive Streams library implementers to validate their implementations against the rules defined in [the Specification](https://github.com/reactive-streams/reactive-streams-jvm).

The TCK is implemented using **plain Java (1.6)** and **TestNG** tests, and should be possible to use from other languages and testing libraries (such as Scala, Groovy, JRuby or others).

## Structure of the TCK

The TCK aims to cover all rules defined in the Specification, however for some rules outlined in the Specification it is
not possible (or viable) to construct automated tests, thus the TCK does not claim to completely verify an implementation, however it is very helpful and is able to validate the most important rules.

The TCK is split up into 4 TestNG test classes which should be extended by implementers, providing their `Publisher` / `Subscriber` implementations for the test harness to validate them. The tests are split in the following way:

* `PublisherVerification`
* `SubscriberWhiteboxVerification`
* `SubscriberBlackboxVerification`
* `IdentityProcessorVerification`

The next sections include examples on how these can be used and describe the various configuration options.

The TCK is provided as binary artifact on [Maven Central](http://search.maven.org/#search|ga|1|reactive-streams-tck):

```xml
<dependency>
  <groupId>org.reactivestreams</groupId>
  <artifactId>reactive-streams-tck</artifactId>
  <version>...</version>
  <scope>test</scope>
</dependency>
```

Please refer to the [Reactive Streams Specification](https://github.com/reactive-streams/reactive-streams-jvm) for the current latest version number. Make sure that the API and TCK dependency versions are equal.

### Test method naming convention

Since the TCK is aimed at Reactive Stream implementers, looking into the sources of the TCK is well expected,
and should help during a libraries implementation cycle.

In order to make mapping between test cases and Specification rules easier, each test case covering a specific
Specification rule abides the following naming convention: `TYPE_spec###_DESC` where:

* `TYPE` is one of: [#type-required](required), [#type-optional](optional), [#type-stochastic](stochastic) or [#type-untested](untested) which describe if this test is covering a Rule that MUST or SHOULD be implemented. The specific words are explained in detail below.
* `###` is the Rule number (`1.xx` Rules are about Publishers, `2.xx` Rules are about Subscribers etc.)
* `DESC` is a short explanation of what exactly is being tested in this test case, as sometimes one Rule may have multiple test cases in order to cover the entire Rule.

Here is an example test method signature:

```java
  // Verifies rule: https://github.com/reactive-streams/reactive-streams-jvm#1.1
  @Test public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
    // ...
  }
```

#### Test types explained:

```java
@Test public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable
```

<a name="type-required"></a>
The `required_` means that this test case is a hard requirement, it covers a *MUST* or *MUST NOT* Rule of the Specification.


```java
@Test public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable
```

<a name="type-optional"></a>
The `optional_` means that this test case is optional, it covers a *MAY* or *SHOULD* Rule of the Specification.
This prefix is also used if more configuration is needed in order to run it, e.g.
`@Additional(implement = "createFailedPublisher") @Test` signals the implementer that in order to run this test
one has to implement the `Publisher<T> createFailedPublisher()` method.

```java
@Test public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable
```

<a name="type-stochastic"></a>
The `stochastic_` means that the Rule is either racy, and/or inherently hard to verify without heavy modification of the tested implementation.
Usually this means that this test case can yield false positives ("be green") even if for some case, the given implementation may violate the tested behaviour.

```java
@Test public void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable
```

<a name="type-untested"></a>
The `untested_` means that the test case is not implemented, either because it is inherently hard to verify (e.g. Rules which use
the wording "*SHOULD consider X as Y*"). Such tests will show up in your test runs as `SKIPPED`, with a message pointing out that the TCK is unable to validate this Rule. If you figure out a way to deterministically test Rules which have been
marked with this prefix – pull requests are encouraged!

### Test isolation

All test assertions are isolated within the required `TestEnvironment`, so it is safe to run the TCK tests in parallel.

### Testing Publishers with restricted capabilities

Some `Publisher`s will not be able to pass through all TCK tests due to some internal or fundamental decissions in their design.
For example, a `FuturePublisher` can be implemented such that it can only ever `onNext` **exactly once** - this means that it is not possible
to run all TCK tests against it, since the tests sometimes require multiple elements to be emitted.

In order to allow such restricted capabilities to be tested against the spec's rules, the TCK provides the `maxElementsFromPublisher()` method
as means of communicating to the TCK the limited capabilities of the Publisher. For example, if a publisher can only ever emit up to `2` elements,
tests in the TCK which require more than 2 elements to verify a rule can be skipped.

In order to inform the TCK your Publisher is only able to signal up to `2` elements, override the `maxElementsFromPublisher` method like this:

```java
@Override public long maxElementsFromPublisher() {
  return 2;
}
```

The TCK also supports Publishers which are not able to signal completion. For example you might have a Publisher being backed by a timer.
Such Publisher does not have a natural way to "complete" after some number of ticks. It would be possible to implement a Processor which would
"take n elements from the TickPublisher and then signal completion to the downstream", but this adds a layer of indirection between the TCK and the
Publisher we initially wanted to test. We suggest testing such unbouded Publishers either way - using a "TakeNElementsProcessor" or by informing the TCK
that the publisher is not able to signal completion. The TCK will then skip all tests which require `onComplete` signals to be emitted.

In order to inform the TCK that your Publiher is not able to signal completion, override the `maxElementsFromPublisher` method like this:

```java
@Override public long maxElementsFromPublisher() {
  return publisherUnableToSignalOnComplete(); // == Long.MAX_VALUE == unbounded
}
```

### Testing a "failed" Publisher
The Reactive Streams spec mandates certain behaviours for Publishers which are "failed",
e.g. it was unable to initialize a connection it needs to emit elements.
It may be useful to specifically such known to be failed Publisher using the TCK.

In order to run additional tests on your failed publisher you can implement the `createFailedPublisher` method.
The expected behaviour from the returned implementation is to follow Rule 1.4 and Rule 1.9 - which are concerned
with the order of emiting the Subscription and signaling the failure.

```java
@Override public Publisher<T> createFailedPublisher() {
  final String invalidData = "this input string is known it to be failed";
  return new MyPublisher(invalidData);
}
```

In case you don't really have a known up-front error state you can put your Publisher into,
you can easily ignore these tests by returning `null` from the `createFailedPublisher` method.
It is important to remember that it is **illegal** to signal `onNext / onComplete / onError` before
you signal the `Subscription`, for details on this rule refer to the Reactive Streams specification.

## Publisher Verification

`PublisherVerification` tests verify Publisher as well as some Subscription Rules of the Specification.

In order to include it's tests in your test suite simply extend it, like this:

```java
package com.example.streams;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class RangePublisherTest extends PublisherVerification<Integer> {

  public RangePublisherTest() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    return new RangePublisher<Integer>(1, elements);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return new Publisher<Integer>() {
      @Override
      public void subscribe(Subscriber<Integer> s) {
        s.onError(new RuntimeException("Can't subscribe subscriber: " + s + ", because of reasons."));
      }
    };
  }

  // ADDITIONAL CONFIGURATION

  @Override
  public long maxElementsFromPublisher() {
    return Long.MAX_VALUE - 1;
  }

  @Override
  public long boundedDepthOfOnNextAndRequestRecursion() {
    return 1;
  }
}
```

Notable configuration options include:

* `maxElementsFromPublisher` – must be overridden in case the Publisher being tested is of bounded length, e.g. it's wrapping a `Future<T>` and thus can only publish up to 1 element, in which case you
  would return `1` from this method. It will cause all tests which require more elements in order to validate a certain
  Rule to be skipped,
* `boundedDepthOfOnNextAndRequestRecursion` – which must be overridden when verifying synchronous Publishers.
  This number returned by this method will be used to validate if a `Subscription` adheres to Rule 3.3 and avoids "unbounded recursion".

### Timeout configuration
Publisher tests make use of two kinds of timeouts, one is the `defaultTimeoutMillis` which corresponds to all methods used
within the TCK which await for something to happen. The other timeout is `publisherReferenceGCTimeoutMillis` which is only used in order to verify
[Rule 3.13](https://github.com/reactive-streams/reactive-streams-jvm#3.13) which defines that subscriber references MUST be dropped
by the Publisher.

In order to configure these timeouts (for example when running on a slow continious integtation machine), you can either:

**Use env variables** to set these timeouts, in which case the you can do:

```bash
export DEFAULT_TIMEOUT_MILLIS=300
export PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS=500
```

Or **define the timeouts explicitly in code**:

```java
public class RangePublisherTest extends PublisherVerification<Integer> {

  public static final long DEFAULT_TIMEOUT_MILLIS = 300L;
  public static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 500L;

  public RangePublisherTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);
  }

  // ...
}
```

Note that explicitly passed in values take precedence over values provided by the environment

## Subscriber Verification

Subscriber rules Verification is split up into two files (styles) of tests.

The Blackbox Verification tests do not require the implementation under test to be modified at all, yet they are *not* able to verify most rules. In Whitebox Verification, more control over `request()` calls etc. is required in order to validate rules more precisely.

### createElement and Helper Publisher implementations
Since testing a `Subscriber` is not possible without a corresponding `Publisher` the TCK Subscriber Verifications
both provide a default "*helper publisher*" to drive its test and also alow to replace this Publisher with a custom implementation.
The helper publisher is an asynchronous publisher by default - meaning that your subscriber can not blindly assume single threaded execution.

While the `Publisher` implementation is provided, creating the signal elements is not – this is because a given Subscriber
may for example only work with `HashedMessage` or some other specific kind of signal. The TCK is unable to generate such
special messages automatically, so we provide the `T createElement(Integer id)` method to be implemented as part of
Subscriber Verifications which should take the given ID and return an element of type `T` (where `T` is the type of
elements flowing into the `Subscriber<T>`, as known thanks to `... extends WhiteboxSubscriberVerification<T>`) representing
an element of the stream that will be passed on to the Subscriber.

The simplest valid implemenation is to return the incoming `id` *as the element* in a verification using `Integer`s as element types:

```java
public class MySubscriberTest extends SubscriberBlackboxVerification<Integer> {

  // ...

  @Override
  public Integer createElement(int element) { return element; }
}
```


The `createElement` method MAY be called from multiple
threads, so in case of more complicated implementations, please be aware of this fact.

**Very Advanced**: While we do not expect many implementations having to do so, it is possible to take full control of the `Publisher`
which will be driving the TCKs test. This can be achieved by implementing the `createHelperPublisher` method in which you can implement your
own Publisher which will then be used by the TCK to drive your Subscriber tests:

```java
@Override public Publisher<Message> createHelperPublisher(long elements) {
  return new Publisher<Message>() { /* IMPL HERE */ };
}
```

### Subscriber Blackbox Verification

Blackbox Verification does not require any additional work except from providing a `Subscriber` and `Publisher` instances to the TCK:

```java
package com.example.streams;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class MySubscriberBlackboxVerificationTest extends SubscriberBlackboxVerification<Integer> {

  public MySubscriberBlackboxVerificationTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber() {
    return new MySubscriber<Integer>();
  }

  @Override
  public Integer createElement(int element) {
    return element;
  }
}
```


### Subscriber Whitebox Verification

The Whitebox Verification tests are able to verify most of the Specification, at the additional cost that control over demand generation and cancellation must be handed over to the TCK via the `SubscriberPuppet`.

Based on experiences so far implementing the `SubscriberPuppet` is non-trivial and can be hard for some implementations.
We keep the whitebox verification, as it is tremendously useful in the `ProcessorVerification`, where the Puppet is implemented within the TCK and injected to the tests.
We do not expect all implementations to make use of the plain `SubscriberWhiteboxVerification`, using the `SubscriberBlackboxVerification` instead.

For the simplest possible (and most common) `Subscriber` implementation using the whitebox verification boils down to
exteding (or delegating to) your implementation with additionally signalling and registering the test probe, as shown in the below example:

```java
package com.example.streams;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class MySubscriberWhiteboxVerificationTest extends SubscriberWhiteboxVerification<Integer> {

  public MySubscriberWhiteboxVerificationTest() {
    super(new TestEnvironment());
  }

  // The implementation under test is "SyncSubscriber":
  // class SyncSubscriber<T> extends Subscriber<T> { /* ... */ }

  @Override
  public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
    // in order to test the SyncSubscriber we must instrument it by extending it,
    // and calling the WhiteboxSubscriberProbe in all of the Subscribers methods:
    return new SyncSubscriber<Integer>() {
      @Override
      public void onSubscribe(final Subscription s) {
        super.onSubscribe(s);

        // register a successful subscription, and create a Puppet,
        // for the WhiteboxVerification to be able to drive its tests:
        probe.registerOnSubscribe(new SubscriberPuppet() {

          @Override
          public void triggerRequest(long elements) {
            s.request(elements);
          }

          @Override
          public void signalCancel() {
            s.cancel();
          }
        });
      }

      @Override
      public void onNext(Integer element) {
        // in addition to normal Subscriber work that you're testing, register onNext with the probe
        super.onNext(element);
        probe.registerOnNext(element);
      }

      @Override
      public void onError(Throwable cause) {
        // in addition to normal Subscriber work that you're testing, register onError with the probe
        super.onError(cause);
        probe.registerOnError(cause);
      }

      @Override
      public void onComplete() {
        // in addition to normal Subscriber work that you're testing, register onComplete with the probe
        super.onComplete();
        probe.registerOnComplete();
      }
    };
  }

  @Override
  public Integer createElement(int element) {
    return element;
  }

}
```

### Timeout configuration
Similarily to `PublisherVerification`, it is possible to set the timeouts used by the TCK to validate subscriber behaviour.
This can be set either by using env variables or hardcoded explicitly.

**Use env variables** to set the timeout value to be used by the TCK:

```bash
export DEFAULT_TIMEOUT_MILLIS=300
```

Or **define the timeout explicitly in code**:

```java
public class MySubscriberTest extends BlackboxSubscriberVerification<Integer> {

  public static final long DEFAULT_TIMEOUT_MILLIS = 300L;

  public RangePublisherTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS));
  }

  // ...
}
```

Note that hard-coded values *take precedence* over environment set values (!).


## Subscription Verification

Please note that while `Subscription` does **not** have it's own test class, it's rules are validated inside of the `Publisher` and `Subscriber` tests – depending if the Rule demands specific action to be taken by the publishing, or subscribing side of the subscription contract.

## Identity Processor Verification

An `IdentityProcessorVerification` tests the given `Processor` for all `Subscriber`, `Publisher` as well as `Subscription` rules. Internally the `WhiteboxSubscriberVerification` is used for `Subscriber` rules.

```java
package com.example.streams;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class MyIdentityProcessorVerificationTest extends IdentityProcessorVerification<Integer> {

  public static final long DEFAULT_TIMEOUT_MILLIS = 300L;
  public static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 1000L;


  public MyIdentityProcessorVerificationTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);
  }

  @Override
  public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
    return new MyIdentityProcessor<Integer, Integer>(bufferSize);
  }

  @Override
  public Publisher<Integer> createHelperPublisher(long elements) {
    return new MyRangePublisher<Integer>(1, elements);
  }

  // ENABLE ADDITIONAL TESTS

  @Override
  public Publisher<Integer> createFailedPublisher() {
    // return Publisher that only signals onError instead of null to run additional tests
    // see this methods JavaDocs for more details on how the returned Publisher should work.
    return null;
  }

  // OPTIONAL CONFIGURATION OVERRIDES
  // override these only if you understand why you'd need to do so for your impl.

  @Override
  public long maxElementsFromPublisher() {
    return super.maxElementsFromPublisher();
  }

  @Override
  public long boundedDepthOfOnNextAndRequestRecursion() {
    return super.boundedDepthOfOnNextAndRequestRecursion();
  }
}
```

The additional configuration options reflect the options available in the Subscriber and Publisher Verifications.

The `IdentityProcessorVerification` also runs additional sanity verifications, which are not directly mapped to Specification rules, but help to verify that a Processor won't "get stuck" or face similar problems. Please refer to the sources for details on the tests included.

## Ignoring tests
Since you inherit these tests instead of defining them yourself it's not possible to use the usual `@Ignore` annotations to skip certain tests
(which may be perfectly reasonable if your implementation has some know constraints on what it cannot implement). We recommend the below pattern
to skip tests inherited from the TCK's base classes:

```java
package com.example.streams;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyIdentityProcessorTest extends IdentityProcessorVerification<Integer> {

  private ExecutorService e;

  @BeforeClass
  public void before() { e = Executors.newFixedThreadPool(4); }

  @AfterClass
  public void after() { if (e != null) e.shutdown(); }

  public SkippingIdentityProcessorTest() {
    super(new TestEnvironment());
  }

  @Override
  public ExecutorService publisherExecutorService() {
    return e;
  }

  @Override
  public Integer createElement(int element) {
    return element;
  }

  @Override
  public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
    return new MyProcessor<Integer, Integer>(buffer Size); // return your implementation
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return null; // returning null means that the tests validating a failed publisher will be skipped
  }

}
```

## Upgrading the TCK to newer versions
While we do not expect the Reactive Streams specification to change in the forseeable future,
it *may happen* that some semantics may need to change at some point. In this case you should expect test
methods being phased out in terms of deprecation or removal, new tests may also be added over time.

In general this should not be of much concern, unless overriding test methods in your test suite.
We ask implementers who find the need of overriding provided test methods to reach out via opening tickets
on the `reactive-streams/reactive-streams-jvm` project, so we can discuss the use case and, most likely, improve the TCK.

## Using the TCK from other languages

The TCK was designed such that it should be possible to consume it using different languages.
The section below shows how to use the TCK using different languages (contributions of examples for more languages are very welcome):

### Scala

In order to run the TCK using [ScalaTest](http://www.scalatest.org/) the test class must mix-in the `TestNGSuiteLike` trait (as of ScalaTest `2.2.x`).

```scala
class IterablePublisherTest(env: TestEnvironment, publisherShutdownTimeout: Long)
  extends PublisherVerification[Int](env, publisherShutdownTimeout)
  with TestNGSuiteLike {

  def this() {
    this(new TestEnvironment(500), 1000)
  }

  def createPublisher(elements: Long): Publisher[Int] = ???

  // example error state publisher implementation
  override def createFailedPublisher(): Publisher[Int] =
    new Publisher[Int] {
      override def subscribe(s: Subscriber[Int]): Unit = {
        s.onError(new Exception("Unable to serve subscribers right now!"))
      }
    }

}
```

### Groovy, JRuby, Kotlin, others...

Contributions to this document are very welcome!

When implementing Reactive Streams using the TCK in some language, please feel free to share an example on how to best use it from your language of choice.
