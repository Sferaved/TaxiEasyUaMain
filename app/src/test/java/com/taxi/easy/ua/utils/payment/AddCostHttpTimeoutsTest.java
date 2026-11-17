package com.taxi.easy.ua.utils.payment;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AddCostHttpTimeoutsTest {

    @Test
    public void cashTimeoutCoversDispatchArchiveWait() {
        assertTrue(AddCostHttpTimeouts.CASH_SECONDS >= 70);
        assertTrue(AddCostHttpTimeouts.WALLET_SECONDS >= AddCostHttpTimeouts.CASH_SECONDS);
    }
}
