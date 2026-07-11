package com.taxi.easy.ua.ui.landing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LandingIntroHelperTest {

    @Test
    public void showsWhenNeverShown() {
        assertTrue(LandingIntroHelper.shouldShowIntroAfterUpdate(0, 1126));
    }

    @Test
    public void showsWhenAppUpdated() {
        assertTrue(LandingIntroHelper.shouldShowIntroAfterUpdate(1125, 1126));
    }

    @Test
    public void skipsWhenAlreadyShownForCurrentVersion() {
        assertFalse(LandingIntroHelper.shouldShowIntroAfterUpdate(1126, 1126));
    }

    @Test
    public void skipsWhenStoredAheadOfCurrent() {
        assertFalse(LandingIntroHelper.shouldShowIntroAfterUpdate(1200, 1126));
    }

    @Test
    public void opensLandingOnColdStart() {
        assertTrue(LandingIntroHelper.shouldOpenLandingOnColdStart(true));
        assertFalse(LandingIntroHelper.shouldOpenLandingOnColdStart(false));
    }
}
