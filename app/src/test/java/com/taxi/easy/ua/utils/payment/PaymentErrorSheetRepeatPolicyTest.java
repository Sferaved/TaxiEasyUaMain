package com.taxi.easy.ua.utils.payment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaymentErrorSheetRepeatPolicyTest {

    @Test
    public void cancelScreenDoesNotRepeatASheetTheUserAlreadySaw() {
        assertFalse(PaymentErrorSheetRepeatPolicy.allowOnCancelScreen(true));
    }

    @Test
    public void cancelScreenShowsTheSheetOnceWhenItWasNotShownYet() {
        assertTrue(PaymentErrorSheetRepeatPolicy.allowOnCancelScreen(false));
    }
}
