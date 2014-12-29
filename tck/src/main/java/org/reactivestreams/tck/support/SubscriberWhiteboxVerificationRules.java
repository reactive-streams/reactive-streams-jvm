package org.reactivestreams.tck.support;

/**
 * Internal TCK use only.
 * Add / Remove tests for PublisherVerificaSubscriberWhiteboxVerification here to make sure that they arre added/removed in the other places.
 */
public interface SubscriberWhiteboxVerificationRules {
  void spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable;
  void spec202_shouldAsynchronouslyDispatch() throws Exception;
  void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable;
  void spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable;
  void spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception;
  void spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception;
  void spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception;
  void spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() throws Exception;
  void spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable;
  void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable;
  void spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable;
  void spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable;
  void spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable;
  void spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception;
  void spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality_specViolation() throws Throwable;
  void spec213_failingOnSignalInvocation() throws Exception;
  void spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception;
  void spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable;
  void spec310_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception;
  void spec311_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception;
  void spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception;
  void spec315_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception;
  void spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception;
}