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
}
