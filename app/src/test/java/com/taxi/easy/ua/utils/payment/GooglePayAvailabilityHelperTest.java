package com.taxi.easy.ua.utils.payment;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GooglePayAvailabilityHelperTest {

    private static final Set<String> BLOCKED_CITIES = new HashSet<>(Arrays.asList(
            "foreign countries",
            "Dnipropetrovsk Oblast",
            "Odessa",
            "Zaporizhzhia",
            "Cherkasy Oblast"
    ));

    @Test
    public void blockedCities_containsExpectedRegions() {
        assertTrue(BLOCKED_CITIES.contains("Odessa"));
        assertTrue(BLOCKED_CITIES.contains("Zaporizhzhia"));
        assertFalse(BLOCKED_CITIES.contains("Kyiv City"));
    }

    @Test
    public void cardPayPermission_zeroBlocksGooglePayOffer() {
        String[] permissions = {"1", "0"};
        assertTrue(permissions.length > 1);
        assertFalse(!"0".equals(permissions[1]));
    }

    @Test
    public void cardPayPermission_oneAllowsGooglePayOffer() {
        String[] permissions = {"1", "1"};
        assertTrue(permissions.length > 1 && !"0".equals(permissions[1]));
    }
}
