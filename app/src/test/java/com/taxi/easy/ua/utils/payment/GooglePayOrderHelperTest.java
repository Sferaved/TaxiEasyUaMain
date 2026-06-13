package com.taxi.easy.ua.utils.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GooglePayOrderHelperTest {

    @Test
    public void isHoldSuccess_acceptsApprovedAndWaitingAuthComplete() {
        assertTrue(GooglePayOrderHelper.isHoldSuccess("Approved"));
        assertTrue(GooglePayOrderHelper.isHoldSuccess("WaitingAuthComplete"));
    }

    @Test
    public void isHoldSuccess_rejectsDeclinedAndEmpty() {
        assertFalse(GooglePayOrderHelper.isHoldSuccess("Declined"));
        assertFalse(GooglePayOrderHelper.isHoldSuccess(""));
    }

    @Test
    public void parseAmountUah_parsesIntegerAndDecimal() {
        assertEquals(120, GooglePayOrderHelper.parseAmountUah("120"));
        assertEquals(99, GooglePayOrderHelper.parseAmountUah("98.6"));
        assertEquals(0, GooglePayOrderHelper.parseAmountUah(""));
        assertEquals(0, GooglePayOrderHelper.parseAmountUah("abc"));
    }
}
