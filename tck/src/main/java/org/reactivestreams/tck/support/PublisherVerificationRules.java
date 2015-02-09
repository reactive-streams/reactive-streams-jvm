package org.reactivestreams.tck.support;


/**
 * Internal TCK use only.
 * Add / Remove tests for PublisherVerification here to make sure that they arre added/removed in the other places.
 */
public interface PublisherVerificationRules {
  void required_validate_maxElementsFromPublisher() throws Exception;
  void required_validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception;
  void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable;
  void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable;
  void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable;
  void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable;
  void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable;
  void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable;
  void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable;
  void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete() throws Throwable;
  void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable;
  void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable;
  void untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable;
  void untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable;
  void untested_spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable;
  void required_spec109_subscribeThrowNPEOnNullSubscriber() throws Throwable;
  void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable;
  void optional_spec111_maySupportMultiSubscribe() throws Throwable;
  void required_spec112_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe() throws Throwable;
  void required_spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable;
  void required_spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable;
  void required_spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable;
  void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable;
  void required_spec303_mustNotAllowUnboundedRecursion() throws Throwable;
  void untested_spec304_requestShouldNotPerformHeavyComputations() throws Exception;
  void untested_spec305_cancelMustNotSynchronouslyPerformHeavyCompuatation() throws Exception;
  void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable;
  void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable;
  void required_spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable;
  void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException() throws Throwable;
  void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable;
  void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable;
  void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable;
  void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable;
  void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable;
}