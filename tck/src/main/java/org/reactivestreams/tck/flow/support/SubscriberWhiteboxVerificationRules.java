/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

package org.reactivestreams.tck.flow.support;

/**
 * Internal TCK use only.
 * Add / Remove tests for PublisherVerificaSubscriberWhiteboxVerification here to make sure that they arre added/removed in the other places.
 */
public interface SubscriberWhiteboxVerificationRules {
  void required_exerciseWhiteboxHappyPath() throws Throwable;
  void required_spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable;
  void untested_spec202_shouldAsynchronouslyDispatch() throws Exception;
  void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable;
  void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable;
  void untested_spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception;
  void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Throwable;
  void untested_spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception;
  void untested_spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() throws Exception;
  void required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable;
  void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable;
  void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable;
  void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable;
  void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable;
  void untested_spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception;
  void untested_spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality_specViolation() throws Throwable;
  void untested_spec213_failingOnSignalInvocation() throws Exception;
  void required_spec213_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  void required_spec213_onNext_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  void required_spec213_onError_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  void untested_spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception;
  void required_spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable;
  void untested_spec310_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception;
  void untested_spec311_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception;
  void untested_spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception;
  void untested_spec315_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception;
  void untested_spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception;
}
