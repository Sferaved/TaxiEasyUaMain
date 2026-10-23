package com.taxi.easy.ua.utils.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapPickGeocodeGateTest {

    @Test
    public void latestSettledPick_isApplied() {
        assertTrue(MapPickGeocodeGate.shouldApplyGeocodeResult(3, 3, false));
    }

    @Test
    public void olderReplyAndActiveGesture_areIgnored() {
        assertFalse(MapPickGeocodeGate.shouldApplyGeocodeResult(2, 3, false));
        assertFalse(MapPickGeocodeGate.shouldApplyGeocodeResult(3, 3, true));
        assertFalse(MapPickGeocodeGate.shouldApplyGeocodeResult(0, 0, false));
    }

    @Test
    public void userPan_doesNotRecenter() {
        assertFalse(MapPickGeocodeGate.shouldRecenterAfterGeocode(true));
        assertTrue(MapPickGeocodeGate.shouldRecenterAfterGeocode(false));
    }
}
