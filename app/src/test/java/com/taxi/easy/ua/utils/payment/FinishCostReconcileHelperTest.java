package com.taxi.easy.ua.utils.payment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinishCostReconcileHelperTest {

    @Test
    public void keepDisplayed_whenServerLagAfterWalletAddCost() {
        assertTrue(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                22, 12, true, false, false, 22, false));
    }

    @Test
    public void keepDisplayed_whenWalletAddCostAppliedEvenWithoutFloor() {
        assertTrue(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                20, 15, true, false, false, null, true));
    }

    @Test
    public void allowServerUpdate_whenServerCaughtUp() {
        assertFalse(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                22, 22, true, false, false, 22, false));
    }

    @Test
    public void keepDisplayed_whileAddCostInFlight() {
        assertTrue(FinishCostReconcileHelper.shouldKeepDisplayedCostOverServer(
                15, 12, false, true, false, null, false));
    }

    @Test
    public void skipOptimisticWalletAdd_whenFloorAlreadyOnScreen() {
        assertTrue(FinishCostReconcileHelper.shouldSkipOptimisticWalletAdd(20, 20, false));
    }

    @Test
    public void skipOptimisticWalletAdd_whenAlreadyApplied() {
        assertTrue(FinishCostReconcileHelper.shouldSkipOptimisticWalletAdd(15, null, true));
    }

    @Test
    public void pickHigherCost_prefersLarger() {
        assertEquals("20", FinishCostReconcileHelper.pickHigherCostGrivna("15", "20"));
        assertEquals("20", FinishCostReconcileHelper.pickHigherCostGrivna("20", "15"));
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

    @Test
    public void capInflatedWalletDisplay_clampsAboveAuthoritative() {
        assertEquals(12, FinishCostReconcileHelper.capInflatedWalletDisplay(17, 12));
        assertEquals(12, FinishCostReconcileHelper.capInflatedWalletDisplay(12, 12));
        assertEquals(12, FinishCostReconcileHelper.capInflatedWalletDisplay(15, 12));
    }

    @Test
    public void applyFinishAbsoluteCostObserver_alwaysForCash() {
        assertTrue(FinishCostReconcileHelper.shouldApplyFinishAbsoluteCostObserver(
                false, "old-uid", false));
    }

    @Test
    public void applyFinishAbsoluteCostObserver_walletWaitsForUidFlag() {
        assertFalse(FinishCostReconcileHelper.shouldApplyFinishAbsoluteCostObserver(
                true, "uid-1", false));
        assertTrue(FinishCostReconcileHelper.shouldApplyFinishAbsoluteCostObserver(
                true, "uid-1", true));
    }
}
