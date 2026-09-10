package com.taxi.easy.ua.utils.location;

import static org.junit.Assert.assertEquals;

import com.taxi.easy.ua.utils.location.GpsButtonAppearanceHelper.Appearance;

import org.junit.Test;

public class GpsButtonAppearanceHelperTest {

    @Test
    public void gpsOff_alwaysRed_evenWithPermissionAndCross() {
        assertEquals(Appearance.RED, GpsButtonAppearanceHelper.resolve(true, false, true));
        assertEquals(Appearance.RED, GpsButtonAppearanceHelper.resolve(false, false, false));
    }

    @Test
    public void gpsOn_noPermission_yellow_evenWithCross() {
        assertEquals(Appearance.YELLOW, GpsButtonAppearanceHelper.resolve(true, true, false));
        assertEquals(Appearance.YELLOW, GpsButtonAppearanceHelper.resolve(false, true, false));
    }

    @Test
    public void gpsOn_withPermission_greenOrCross() {
        assertEquals(Appearance.GREEN_CROSS, GpsButtonAppearanceHelper.resolve(true, true, true));
        assertEquals(Appearance.GREEN, GpsButtonAppearanceHelper.resolve(false, true, true));
    }
}
