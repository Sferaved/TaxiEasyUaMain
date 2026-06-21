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

    @Test
    public void parseAmountUah_parsesAddCostDeltaOnly() {
        assertEquals(5, GooglePayOrderHelper.parseAmountUah("5"));
        assertEquals(5, GooglePayOrderHelper.parseAmountUah(" 5 "));
    }

    @Test
    public void isChargeServerError_detectsHttp5xxCodes() {
        assertTrue(GooglePayOrderHelper.isChargeServerError("charge_http_500"));
        assertTrue(GooglePayOrderHelper.isChargeServerError("charge_http_503"));
        assertFalse(GooglePayOrderHelper.isChargeServerError("charge_http_400"));
        assertFalse(GooglePayOrderHelper.isChargeServerError("Declined"));
    }

    @Test
    public void isChargeNetworkError_detectsNetworkFailures() {
        assertTrue(GooglePayOrderHelper.isChargeNetworkError("network_error"));
        assertFalse(GooglePayOrderHelper.isChargeNetworkError("charge_http_500"));
    }

    @Test
    public void isDuplicateOrderError_detectsDuplicateOrderIdCode() {
        assertTrue(GooglePayOrderHelper.isDuplicateOrderError("duplicate_order_id"));
        assertFalse(GooglePayOrderHelper.isDuplicateOrderError("Declined"));
    }

    @Test
    public void isTimeoutFailure_detectsTimeoutMessages() {
        assertTrue(GooglePayOrderHelper.isTimeoutFailure("timeout"));
        assertTrue(GooglePayOrderHelper.isTimeoutFailure("Read timed out"));
        assertFalse(GooglePayOrderHelper.isTimeoutFailure("Declined"));
    }

    @Test
    public void isRecoverableHoldFailure_coversTimeoutNetworkAnd5xx() {
        assertTrue(GooglePayOrderHelper.isRecoverableHoldFailure("timeout"));
        assertTrue(GooglePayOrderHelper.isRecoverableHoldFailure("charge_http_500"));
        assertTrue(GooglePayOrderHelper.isRecoverableHoldFailure("network_error"));
        assertFalse(GooglePayOrderHelper.isRecoverableHoldFailure("Declined"));
    }

    @Test
    public void usesWalletHold_coversWfpAndGooglePay() {
        assertTrue(PaymentTypeHelper.usesWalletHold(PaymentTypeHelper.CARD));
        assertTrue(PaymentTypeHelper.usesWalletHold(PaymentTypeHelper.GOOGLE_PAY));
        assertFalse(PaymentTypeHelper.usesWalletHold(PaymentTypeHelper.NAL));
    }
}

