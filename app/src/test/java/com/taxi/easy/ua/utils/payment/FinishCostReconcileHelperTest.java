package com.taxi.easy.ua.utils.payment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinishCostReconcileHelperTest {

    @Test
    public void keepDisplayed_whenServerLagAfterWalletAddCost() {
        assertTrue(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                22, 12, true, false, false, 22));
    }

    @Test
    public void allowServerUpdate_whenServerCaughtUp() {
        assertFalse(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                22, 22, true, false, false, 22));
    }

    @Test
    public void keepDisplayed_whileAddCostInFlight() {
        assertTrue(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                15, 12, false, true, false, null));
    }

    @Test
    public void computeOptimisticWalletTotal_addsDelta() {
        assertEquals(22, FinishCostReconcileHelper.computeOptimisticWalletTotal(12, 10, null));
    }

    @Test
    public void serverConfirmedWalletFloor_whenServerAtOrAboveFloor() {
        assertTrue(FinishCostReconcileHelper.serverConfirmedWalletFloor(22, 22));
        assertFalse(FinishCostReconcileHelper.serverConfirmedWalletFloor(12, 22));
    }
}
