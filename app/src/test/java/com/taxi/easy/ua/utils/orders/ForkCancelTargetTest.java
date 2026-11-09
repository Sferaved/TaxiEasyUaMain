package com.taxi.easy.ua.utils.orders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForkCancelTargetTest {

    @Test
    public void blankMarkers_areNotAForkLeg() {
        assertEquals("", ForkCancelTarget.normalizeDoubleUid(null));
        assertEquals("", ForkCancelTarget.normalizeDoubleUid(""));
        assertEquals("", ForkCancelTarget.normalizeDoubleUid(" "));
        assertEquals("", ForkCancelTarget.normalizeDoubleUid("*"));
        assertEquals("", ForkCancelTarget.normalizeDoubleUid("null"));
        assertFalse(ForkCancelTarget.hasLinkedDouble(" ", "*"));
    }

    @Test
    public void liveUid_winsOverPersisted() {
        assertEquals("a2bced741a124cb0acdd7843c1e089c8",
                ForkCancelTarget.resolveDoubleUid(
                        " a2bced741a124cb0acdd7843c1e089c8 ",
                        "old-double"));
        assertTrue(ForkCancelTarget.hasLinkedDouble(
                "a2bced741a124cb0acdd7843c1e089c8", null));
    }

    @Test
    public void persistedUid_usedWhenLiveIsBlank() {
        assertEquals("a2bced741a124cb0acdd7843c1e089c8",
                ForkCancelTarget.resolveDoubleUid(" ", "a2bced741a124cb0acdd7843c1e089c8"));
        assertTrue(ForkCancelTarget.hasLinkedDouble(null, "a2bced741a124cb0acdd7843c1e089c8"));
    }

    @Test
    public void cashLegUid_isNotAdoptedAsNewOrder() {
        String card = "1f7c6f637b7e42839dd8c8984ff11683";
        String cash = "62bfdb9c56fc46d2a781fd130fd60e65";
        assertTrue(ForkCancelTarget.isForkPartnerStatusUid(card, cash, cash));
        assertFalse(ForkCancelTarget.isForkPartnerStatusUid(card, cash, card));
        assertEquals(card, ForkCancelTarget.statusUidToAdopt(card, cash, cash, card));
        assertEquals(card, ForkCancelTarget.statusUidToAdopt(card, cash, cash, null));
    }

    @Test
    public void recreatedOrderUid_isAdopted() {
        String card = "1f7c6f637b7e42839dd8c8984ff11683";
        String cash = "62bfdb9c56fc46d2a781fd130fd60e65";
        String recreated = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        assertEquals(recreated, ForkCancelTarget.statusUidToAdopt(card, cash, recreated, recreated));
    }
}
