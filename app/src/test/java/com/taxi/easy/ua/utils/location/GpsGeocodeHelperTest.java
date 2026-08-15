package com.taxi.easy.ua.utils.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GpsGeocodeHelperTest {

    private static final String START = "Місце посадки";
    private static final String FINISH = "Місце призначення";

    @Test
    public void finishPlaceholder_usesDestinationLabel() {
        assertEquals(FINISH, GpsGeocodeHelper.resolveMapPointLabel(null, true, START, FINISH));
        assertEquals(FINISH, GpsGeocodeHelper.resolveMapPointLabel("", true, START, FINISH));
        assertEquals(FINISH, GpsGeocodeHelper.resolveMapPointLabel("Точка на карте", true, START, FINISH));
        assertEquals(FINISH, GpsGeocodeHelper.resolveMapPointLabel("Точка на карті", true, START, FINISH));
        assertEquals(FINISH, GpsGeocodeHelper.resolveMapPointLabel(START, true, START, FINISH));
    }

    @Test
    public void startPlaceholder_usesPickupLabel() {
        assertEquals(START, GpsGeocodeHelper.resolveMapPointLabel(null, false, START, FINISH));
        assertEquals(START, GpsGeocodeHelper.resolveMapPointLabel("Точка на карте", false, START, FINISH));
        assertEquals(START, GpsGeocodeHelper.resolveMapPointLabel(FINISH, false, START, FINISH));
    }

    @Test
    public void realAddress_keptAsIs() {
        assertEquals("вул. Хрещатик 1",
                GpsGeocodeHelper.resolveMapPointLabel("вул. Хрещатик 1", true, START, FINISH));
        assertEquals("вул. Хрещатик 1",
                GpsGeocodeHelper.resolveMapPointLabel("  вул. Хрещатик 1  ", false, START, FINISH));
    }

    @Test
    public void serverPointOnMap_detectedInBothLanguages() {
        assertTrue(GpsGeocodeHelper.isServerPointOnMapPlaceholder("Точка на карте"));
        assertTrue(GpsGeocodeHelper.isServerPointOnMapPlaceholder("Точка на карті, парк"));
        assertFalse(GpsGeocodeHelper.isServerPointOnMapPlaceholder("вул. Тест 12"));
        assertFalse(GpsGeocodeHelper.isServerPointOnMapPlaceholder(null));
    }

    @Test
    public void unresolvedAddress_coversEmptyAndRoleLabels() {
        assertTrue(GpsGeocodeHelper.isUnresolvedMapAddress(null, START, FINISH));
        assertTrue(GpsGeocodeHelper.isUnresolvedMapAddress("   ", START, FINISH));
        assertTrue(GpsGeocodeHelper.isUnresolvedMapAddress(START, START, FINISH));
        assertTrue(GpsGeocodeHelper.isUnresolvedMapAddress(FINISH, START, FINISH));
        assertFalse(GpsGeocodeHelper.isUnresolvedMapAddress("парк Шевченка", START, FINISH));
    }
}
