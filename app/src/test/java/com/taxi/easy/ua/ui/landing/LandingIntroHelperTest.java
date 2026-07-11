package com.taxi.easy.ua.ui.landing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LandingIntroHelperTest {

    @Test
    public void showsWhenNeverShown_guestOnly() {
        assertTrue(LandingIntroHelper.shouldShowIntroAfterUpdate(0, 1126, true));
        assertFalse(LandingIntroHelper.shouldShowIntroAfterUpdate(0, 1126, false));
    }

    @Test
    public void showsWhenAppUpdated_guestOnly() {
        assertTrue(LandingIntroHelper.shouldShowIntroAfterUpdate(1125, 1126, true));
        assertFalse(LandingIntroHelper.shouldShowIntroAfterUpdate(1125, 1126, false));
    }

    @Test
    public void skipsWhenAlreadyShownForCurrentVersion() {
        assertFalse(LandingIntroHelper.shouldShowIntroAfterUpdate(1126, 1126, true));
    }

    @Test
    public void skipsWhenStoredAheadOfCurrent() {
        assertFalse(LandingIntroHelper.shouldShowIntroAfterUpdate(1200, 1126, true));
    }

    @Test
    public void opensLandingOnColdStart_guestOnly() {
        assertTrue(LandingIntroHelper.shouldOpenLandingOnColdStart(true, true));
        assertFalse(LandingIntroHelper.shouldOpenLandingOnColdStart(true, false));
        assertFalse(LandingIntroHelper.shouldOpenLandingOnColdStart(false, true));
        assertTrue(LandingIntroHelper.shouldEnterOrderOnColdStart(true, false));
        assertFalse(LandingIntroHelper.shouldEnterOrderOnColdStart(true, true));
    }
}
