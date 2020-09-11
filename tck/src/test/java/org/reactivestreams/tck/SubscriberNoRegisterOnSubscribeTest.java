/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck;

import org.reactivestreams.*;
import org.testng.annotations.Test;

/**
 * This test verifies that the SubscriberWhiteboxVerification reports that
 * WhiteboxSubscriberProbe.registerOnSubscribe was not called during the setup
 * of each test.
 */
@Test
public class SubscriberNoRegisterOnSubscribeTest extends SubscriberWhiteboxVerification<Integer> {

    public SubscriberNoRegisterOnSubscribeTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
        return new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
                // deliberately not calling probe.registerOnSubscribe()
            }

            @Override
            public void onNext(Integer integer) {
                probe.registerOnNext(integer);
            }

            @Override
            public void onError(Throwable t) {
                probe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

    void assertMessage(AssertionError ex) {
        String message = ex.toString();
        if (!message.contains(("did not `registerOnSubscribe` within"))) {
            throw ex;
        }
    }

    @Test
    @Override
    public void required_exerciseWhiteboxHappyPath() throws Throwable {
        try {
            super.required_exerciseWhiteboxHappyPath();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable {
        try {
            super.required_spec201_mustSignalDemandViaSubscriptionRequest();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Throwable {
        try {
            super.required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable {
        try {
            super.required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
        try {
            super.required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
        try {
            super.required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable {
        try {
            super.required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
        try {
            super.required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec213_onError_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable {
        try {
            super.required_spec213_onError_mustThrowNullPointerExceptionWhenParametersAreNull();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec213_onNext_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable {
        try {
            super.required_spec213_onNext_mustThrowNullPointerExceptionWhenParametersAreNull();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec213_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable {
        try {
            super.required_spec213_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }

    @Test
    @Override
    public void required_spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
        try {
            super.required_spec308_requestMustRegisterGivenNumberElementsToBeProduced();
        } catch (AssertionError ex) {
            assertMessage(ex);
        }
    }
}
