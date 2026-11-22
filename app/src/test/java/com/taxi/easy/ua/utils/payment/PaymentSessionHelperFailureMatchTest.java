package com.taxi.easy.ua.utils.payment;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Mantis #124: pay_error прошлого заказа не должен цепляться к новому uid. */
public class PaymentSessionHelperFailureMatchTest {

    @Test
    public void failureMatchesOnlySameUid() {
        assertTrue(PaymentSessionHelper.failureMatchesOrder(
                true, "8d9e1a23aaaaaaaaaaaaaaaaaaaaaaaa", "8d9e1a23aaaaaaaaaaaaaaaaaaaaaaaa"));
        assertFalse(PaymentSessionHelper.failureMatchesOrder(
                true, "8d9e1a23aaaaaaaaaaaaaaaaaaaaaaaa", "88e4f94ffac34c54b250c6b0a971f924"));
        assertFalse(PaymentSessionHelper.failureMatchesOrder(
                true, "", "88e4f94ffac34c54b250c6b0a971f924"));
        assertFalse(PaymentSessionHelper.failureMatchesOrder(
                false, "8d9e1a23aaaaaaaaaaaaaaaaaaaaaaaa", "8d9e1a23aaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test
    public void emptyOrderUid_keepsGlobalFlagBehavior() {
        assertTrue(PaymentSessionHelper.failureMatchesOrder(true, "x", null));
        assertTrue(PaymentSessionHelper.failureMatchesOrder(true, "x", ""));
        assertFalse(PaymentSessionHelper.failureMatchesOrder(false, "x", null));
    }
}
