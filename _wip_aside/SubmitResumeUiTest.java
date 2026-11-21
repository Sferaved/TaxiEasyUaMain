package com.taxi.easy.ua.utils.payment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubmitResumeUiTest {

    @Test
    public void cardSubmitDoesNotShowGooglePayScreen() {
        assertFalse(SubmitResumeUi.shouldShowGooglePayProcessing(
                PaymentTypeHelper.CARD, false));
    }

    @Test
    public void googlePayHoldShowsGooglePayScreen() {
        assertTrue(SubmitResumeUi.shouldShowGooglePayProcessing(
                PaymentTypeHelper.GOOGLE_PAY, true));
        assertTrue(SubmitResumeUi.shouldShowGooglePayProcessing(
                PaymentTypeHelper.GOOGLE_PAY, false));
    }

    @Test
    public void holdInProgressShowsGooglePayScreenBeforePayMethodIsStored() {
        assertTrue(SubmitResumeUi.shouldShowGooglePayProcessing(null, true));
    }
}
