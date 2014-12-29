package org.reactivestreams.tck.support;

/**
 * Internal TCK use only.
 * Add / Remove tests for PublisherVerification here to make sure that they arre added/removed in the other places.
 */
public interface PublisherVerificationRules {
  void spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable;
  void spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable;
  void spec103_mustSignalOnMethodsSequentially() throws Throwable;
  void spec104_mustSignalOnErrorWhenFails() throws Throwable;
  void spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable;
  void spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable;
  void spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable;
  void spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable;
  void spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable;
  void spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable;
  void spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable;
  void spec111_maySupportMultiSubscribe() throws Throwable;
  void spec112_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorInsteadOfOnSubscribe() throws Throwable;
  void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable;
  void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable;
  void spec113_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable;
  void spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable;
  void spec303_mustNotAllowUnboundedRecursion() throws Throwable;
  void spec304_requestShouldNotPerformHeavyComputations() throws Exception;
  void spec305_cancelMustNotSynchronouslyPerformHeavyCompuatation() throws Exception;
  void spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable;
  void spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable;
  void spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable;
  void spec309_requestNegativeNumberMustSignalIllegalArgumentException() throws Throwable;
  void spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable;
  void spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable;
  void spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable;
  void spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable;
  void spec317_mustSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable;
}