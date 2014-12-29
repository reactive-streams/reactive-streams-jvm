package org.reactivestreams.tck.support;

/**
 * Internal TCK use only.
 * Add / Remove tests for SubscriberBlackboxVerification here to make sure that they arre added/removed in the other places.
 */
public interface SubscriberBlackboxVerificationRules {
  void spec201_blackbox_mustSignalDemandViaSubscriptionRequest() throws Throwable;
  void spec202_blackbox_shouldAsynchronouslyDispatch() throws Exception;
  void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable;
  void spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable;
  void spec204_blackbox_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception;
  void spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception;
  void spec206_blackbox_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception;
  void spec207_blackbox_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() throws Exception;
  void spec208_blackbox_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable;
  void spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable;
  void spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable;
  void spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable;
  void spec211_blackbox_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception;
  void spec212_blackbox_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable;
  void spec213_blackbox_failingOnSignalInvocation() throws Exception;
  void spec301_blackbox_mustNotBeCalledOutsideSubscriberContext() throws Exception;
  void spec308_blackbox_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable;
  void spec310_blackbox_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception;
  void spec311_blackbox_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception;
  void spec314_blackbox_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception;
  void spec315_blackbox_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception;
  void spec316_blackbox_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception;
}