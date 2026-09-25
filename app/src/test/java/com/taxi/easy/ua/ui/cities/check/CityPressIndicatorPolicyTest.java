package com.taxi.easy.ua.ui.cities.check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CityPressIndicatorPolicyTest {

    @Test
    public void indicatorRunsForSixtySeconds() {
        assertEquals(60_000L, CityPressIndicatorPolicy.DURATION_MS);
    }

    @Test
    public void everyChosenCityShowsMovingIndicator() {
        assertTrue(CityPressIndicatorPolicy.shouldShow("OdessaTest"));
        assertTrue(CityPressIndicatorPolicy.shouldShow("Odessa"));
        assertTrue(CityPressIndicatorPolicy.shouldShow("Kyiv City"));
        assertFalse(CityPressIndicatorPolicy.shouldShow(null));
        assertFalse(CityPressIndicatorPolicy.shouldShow("  "));
    }
}
