# Release notes for Reactive Streams

---

# Version 1.0.2 released on 2017-12-19

## Announcement:

We—the Reactive Streams community—are pleased to announce the immediate availability of `Reactive Streams 1.0.2`. This update to `Reactive Streams` brings the following improvements over `1.0.1`.

## Highlights:

- Specification
  + Glossary term added for `Thread-safe`
  + No breaking/semantical changes
  + Rule [clarifications](#specification-clarifications-102)
- Interfaces
  + No changes
- Technology Compatibility Kit (TCK)
  + Improved [coverage](#tck-alterations-102)
    * Supports Publishers/Processors which do [coordinated emission](http://www.reactive-streams.org/reactive-streams-tck-1.0.2-javadoc/org/reactivestreams/tck/PublisherVerification.html#doesCoordinatedEmission--).
  + Improved JavaDoc
- Examples
  + New example [RangePublisher](http://www.reactive-streams.org/reactive-streams-examples-1.0.2-javadoc/org/reactivestreams/example/unicast/RangePublisher.html)
- Artifacts
  + NEW! [Flow adapters](#flow-adapters)
  + NEW! [Flow TCK](#flow-tck)
  + Java 9 [Automatic-Module-Name](#automatic-module-name) added for all artifacts

## Specification clarifications 1.0.2

## Subscriber Rule 2

**1.0.1:** The intent of this rule is that a Subscriber should not obstruct the progress of the Publisher from an execution point-of-view. In other words, the Subscriber should not starve the Publisher from CPU cycles.

**1.0.2:** The intent of this rule is that a Subscriber should not obstruct the progress of the Publisher from an execution point-of-view. In other words, the Subscriber should not starve the Publisher from receiving CPU cycles.

## Subscriber Rule 8

**1.0.1:** The intent of this rule is to highlight that there may be a delay between calling `cancel` the Publisher seeing that.

**1.0.2** The intent of this rule is to highlight that there may be a delay between calling `cancel` and the Publisher observing that cancellation.

## Flow adapters

An adapter library has been created to convert `org.reactivestreams` to `java.util.concurrent.Flow` and vice versa. Read more about it [here](http://www.reactive-streams.org/reactive-streams-flow-adapters-1.0.2-javadoc)

~~~xml
<dependency>
   <groupId>org.reactivestreams</groupId>
   <artifactId>reactive-streams-flow-adapters</artifactId>
  <version>1.0.2</version>
</dependency>
~~~

## Flow TCK

A TCK artifact has been created to allow for direct TCK verification of `java.util.concurrent.Flow` implementations. Read more about it [here](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.2/tck-flow/README.md)

~~~xml
<dependency>
   <groupId>org.reactivestreams</groupId>
   <artifactId>reactive-streams-tck-flow</artifactId>
  <version>1.0.2</version>
</dependency>
~~~

## Automatic Module Name

  * `org.reactivestreams:reactive-streams` => `org.reactivestreams`
  * `org.reactivestreams:reactive-streams-examples` => `org.reactivestreams.examples`
  * `org.reactivestreams:reactive-streams-tck` => `org.reactivestreams.tck`
  * `org.reactivestreams:reactive-streams-flow-adapters` => `org.reactivestreams.flowadapters`
  * `org.reactivestreams:reactive-streams-tck-flow` => `org.reactivestreams.tckflow`

## TCK alterations 1.0.2

- Added support for Publisher verification of Publishers who do coordinated emission, i.e. where elements only are emitted after all current Subscribers have signalled demand. ([#284](https://github.com/reactive-streams/reactive-streams-jvm/issues/284))
- The `SubscriberWhiteboxVerification` has been given more user friendly error messages in the case where the user forgets to call `registerOnSubscribe`. (#416)[https://github.com/reactive-streams/reactive-streams-jvm/pull/416]

## Contributors
  + Roland Kuhn [(@rkuhn)](https://github.com/rkuhn)
  + Ben Christensen [(@benjchristensen)](https://github.com/benjchristensen)
  + Viktor Klang [(@viktorklang)](https://github.com/viktorklang)
  + Stephane Maldini [(@smaldini)](https://github.com/smaldini)
  + Stanislav Savulchik [(@savulchik)](https://github.com/savulchik)
  + Konrad Malawski [(@ktoso)](https://github.com/ktoso)
  + Slim Ouertani [(@ouertani)](https://github.com/ouertani)
  + Martynas Mickevičius [(@2m)](https://github.com/2m)
  + Luke Daley [(@ldaley)](https://github.com/ldaley)
  + Colin Godsey [(@colinrgodsey)](https://github.com/colinrgodsey)
  + David Moten [(@davidmoten)](https://github.com/davidmoten)
  + Brian Topping [(@briantopping)](https://github.com/briantopping)
  + Rossen Stoyanchev [(@rstoyanchev)](https://github.com/rstoyanchev)
  + Björn Hamels [(@BjornHamels)](https://github.com/BjornHamels)
  + Jake Wharton [(@JakeWharton)](https://github.com/JakeWharton)
  + Anthony Vanelverdinghe[(@anthonyvdotbe)](https://github.com/anthonyvdotbe)
  + Kazuhiro Sera [(@seratch)](https://github.com/seratch)
  + Dávid Karnok [(@akarnokd)](https://github.com/akarnokd)
  + Evgeniy Getman [(@egetman)](https://github.com/egetman)
  + Ángel Sanz [(@angelsanz)](https://github.com/angelsanz)
  + (new) shenghaiyang [(@shenghaiyang)](https://github.com/shenghaiyang)
  + (new) Kyle Thomson [(@kiiadi)](https://github.com/kiiadi)


---

# Version 1.0.1 released on 2017-08-09

## Announcement: 

After more than two years since 1.0.0, we are proud to announce the immediate availability of `Reactive Streams version 1.0.1`.

Since 1.0.0 was released `Reactive Streams` has managed to achieve most, if not all, it set out to achieve. There are now numerous implementations, and it is scheduled to be included in [JDK9](http://download.java.net/java/jdk9/docs/api/java/util/concurrent/Flow.html).

Also, most importantly, there are no semantical incompatibilities included in this release.

When JDK9 ships, `Reactive Streams` will publish a compatibility/conversion library to seamlessly convert between the `java.util.concurrent.Flow` and the `org.reactivestreams` namespaces.

## Highlights:

- Specification
  + A new [Glossary](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.1/README.md#glossary) section
  + Description of the intent behind every single rule
  + No breaking semantical changes
  + Multiple rule [clarifications](#specification-clarifications-1.0.1)
- Interfaces
  + No changes
  + Improved JavaDoc
- Technology Compatibility Kit (TCK)
  + Improved coverage
  + Improved JavaDoc
  + Multiple test [alterations](#tck-alterations-1.0.1)

## Specification clarifications 1.0.1

## Publisher Rule 1

**1.0.0:** The total number of onNext signals sent by a Publisher to a Subscriber MUST be less than or equal to the total number of elements requested by that Subscriber´s Subscription at all times.

**1.0.1:** The total number of onNext´s signalled by a Publisher to a Subscriber MUST be less than or equal to the total number of elements requested by that Subscriber´s Subscription at all times.

**Comment: Minor spelling update.**

## Publisher Rule 2

**1.0.0:** A Publisher MAY signal less onNext than requested and terminate the Subscription by calling onComplete or onError.

**1.0.1:** A Publisher MAY signal fewer onNext than requested and terminate the Subscription by calling onComplete or onError.

**Comment: Minor spelling update.**

## Publisher Rule 3

**1.0.0:** onSubscribe, onNext, onError and onComplete signaled to a Subscriber MUST be signaled sequentially (no concurrent notifications).

**1.0.1:** onSubscribe, onNext, onError and onComplete signaled to a Subscriber MUST be signaled in a thread-safe manner—and if performed by multiple threads—use external synchronization.

**Comment: Reworded the part about sequential signal and its implications, for clarity.**

## Subscriber Rule 6

**1.0.0:** A Subscriber MUST call Subscription.cancel() if it is no longer valid to the Publisher without the Publisher having signaled onError or onComplete.

**1.0.1:** A Subscriber MUST call Subscription.cancel() if the Subscription is no longer needed.

**Comment: Rule could be reworded since it now has an intent section describing desired effect.**

## Subscriber Rule 11

**1.0.0:** A Subscriber MUST make sure that all calls on its onXXX methods happen-before [1] the processing of the respective signals. I.e. the Subscriber must take care of properly publishing the signal to its processing logic.

**1.0.1:** A Subscriber MUST make sure that all calls on its signal methods happen-before the processing of the respective signals. I.e. the Subscriber must take care of properly publishing the signal to its processing logic.

**Comment: Rule slightly reworded to use the glossary for `signal` instead of the more *ad-hoc* name "onXXX methods". Footnote was reworked into the Intent-section of the rule.**

## Subscription Rule 1

**1.0.0:** Subscription.request and Subscription.cancel MUST only be called inside of its Subscriber context. A Subscription represents the unique relationship between a Subscriber and a Publisher [see 2.12].

**1.0.1:** Subscription.request and Subscription.cancel MUST only be called inside of its Subscriber context.

**Comment: Second part of rule moved into the Intent-section of the rule.**

## Subscription Rule 3

**1.0.0:** Subscription.request MUST place an upper bound on possible synchronous recursion between Publisher and Subscriber[1].

**1.0.1:** Subscription.request MUST place an upper bound on possible synchronous recursion between Publisher and Subscriber.

**Comment: Footnote reworked into the Intent-section of the rule.**

## Subscription Rule 4

**1.0.0:** Subscription.request SHOULD respect the responsivity of its caller by returning in a timely manner[2].

**1.0.1:** Subscription.request SHOULD respect the responsivity of its caller by returning in a timely manner.

**Comment: Footnote reworked into the Intent-section of the rule.**

## Subscription Rule 5

**1.0.0:** Subscription.cancel MUST respect the responsivity of its caller by returning in a timely manner[2], MUST be idempotent and MUST be thread-safe.

**1.0.1:** Subscription.cancel MUST respect the responsivity of its caller by returning in a timely manner, MUST be idempotent and MUST be thread-safe.

**Comment: Footnote reworked into the Intent-section of the rule.**

## Subscription Rule 9

**1.0.0:** While the Subscription is not cancelled, Subscription.request(long n) MUST signal onError with a java.lang.IllegalArgumentException if the argument is <= 0. The cause message MUST include a reference to this rule and/or quote the full rule.

**1.0.1:** While the Subscription is not cancelled, Subscription.request(long n) MUST signal onError with a java.lang.IllegalArgumentException if the argument is <= 0. The cause message SHOULD explain that non-positive request signals are illegal.

**Comment: The MUST requirement to include a reference to the rule in the exception message has been dropped, in favor of that the exception message SHOULD explain that non-positive requests are illegal.**

## Subscription Rule 13

**1.0.0:** While the Subscription is not cancelled, Subscription.cancel() MUST request the Publisher to eventually drop any references to the corresponding subscriber. Re-subscribing with the same Subscriber object is discouraged [see 2.12], but this specification does not mandate that it is disallowed since that would mean having to store previously cancelled subscriptions indefinitely.

**1.0.1:** While the Subscription is not cancelled, Subscription.cancel() MUST request the Publisher to eventually drop any references to the corresponding subscriber.

**Comment: Second part of rule reworked into the Intent-section of the rule.**

## Subscription Rule 15

**1.0.0:** Calling Subscription.cancel MUST return normally. The only legal way to signal failure to a Subscriber is via the onError method.

**1.0.1:** Calling Subscription.cancel MUST return normally.

**Comment: Replaced second part of rule with a definition for `return normally` in the glossary.**

## Subscription Rule 16

**1.0.0:** Calling Subscription.request MUST return normally. The only legal way to signal failure to a Subscriber is via the onError method.

**1.0.1:** Calling Subscription.request MUST return normally.

**Comment: Replaced second part of rule with a definition for `return normally` in the glossary.**

## Subscription Rule 17

**1.0.0:** A Subscription MUST support an unbounded number of calls to request and MUST support a demand (sum requested - sum delivered) up to 2^63-1 (java.lang.Long.MAX_VALUE). A demand equal or greater than 2^63-1 (java.lang.Long.MAX_VALUE) MAY be considered by the Publisher as “effectively unbounded”[3].

**1.0.1:** A Subscription MUST support an unbounded number of calls to request and MUST support a demand up to 2^63-1 (java.lang.Long.MAX_VALUE). A demand equal or greater than 2^63-1 (java.lang.Long.MAX_VALUE) MAY be considered by the Publisher as “effectively unbounded”.

**Comment: Rule simplified by defining `demand` in the glossary, and footnote was reworked into the Intent-section of the rule.**

---

## TCK alterations 1.0.1

- Fixed potential resource leaks in partially consuming Publisher tests ([#375](https://github.com/reactive-streams/reactive-streams-jvm/issues/375))
- Fixed potential resource leaks in partially emitting Subscriber tests ([#372](https://github.com/reactive-streams/reactive-streams-jvm/issues/372), [#373](https://github.com/reactive-streams/reactive-streams-jvm/issues/373))
- Renamed `untested_spec305_cancelMustNotSynchronouslyPerformHeavyCompuatation` to `untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation` ([#306](https://github.com/reactive-streams/reactive-streams-jvm/issues/306))
- Allow configuring separate timeout for "no events during N time", allowing for more aggressive timeouts in the rest of the test suite if required ([#314](https://github.com/reactive-streams/reactive-streams-jvm/issues/314))
- New test verifying Rule 2.10, in which subscriber must be prepared to receive onError signal without having signaled request before ([#374](https://github.com/reactive-streams/reactive-streams-jvm/issues/374))
- The verification of Rule 3.9 has been split up into 2 different tests, one to verify that an IllegalArgumentException is sent, and the other an optional check to verify that the exception message informs that non-positive request signals are illegal.
---

## Contributors
  + Roland Kuhn [(@rkuhn)](https://github.com/rkuhn)
  + Ben Christensen [(@benjchristensen)](https://github.com/benjchristensen)
  + Viktor Klang [(@viktorklang)](https://github.com/viktorklang)
  + Stephane Maldini [(@smaldini)](https://github.com/smaldini)
  + Stanislav Savulchik [(@savulchik)](https://github.com/savulchik)
  + Konrad Malawski [(@ktoso)](https://github.com/ktoso)
  + Slim Ouertani [(@ouertani)](https://github.com/ouertani)
  + Martynas Mickevičius [(@2m)](https://github.com/2m)
  + Luke Daley [(@ldaley)](https://github.com/ldaley)
  + Colin Godsey [(@colinrgodsey)](https://github.com/colinrgodsey)
  + David Moten [(@davidmoten)](https://github.com/davidmoten)
  + (new) Brian Topping [(@briantopping)](https://github.com/briantopping)
  + (new) Rossen Stoyanchev [(@rstoyanchev)](https://github.com/rstoyanchev)
  + (new) Björn Hamels [(@BjornHamels)](https://github.com/BjornHamels)
  + (new) Jake Wharton [(@JakeWharton)](https://github.com/JakeWharton)
  + (new) Anthony Vanelverdinghe[(@anthonyvdotbe)](https://github.com/anthonyvdotbe)
  + (new) Kazuhiro Sera [(@seratch)](https://github.com/seratch)
  + (new) Dávid Karnok [(@akarnokd)](https://github.com/akarnokd)
  + (new) Evgeniy Getman [(@egetman)](https://github.com/egetman)
  + (new) Ángel Sanz [(@angelsanz)](https://github.com/angelsanz)
