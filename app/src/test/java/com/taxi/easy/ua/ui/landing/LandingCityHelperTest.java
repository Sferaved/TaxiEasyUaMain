package com.taxi.easy.ua.ui.landing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LandingCityHelperTest {

    private SharedPreferencesHelper prefs;

    @Before
    public void setUp() {
        prefs = new SharedPreferencesHelper(RuntimeEnvironment.getApplication());
        prefs.clear();
    }

    @Test
    public void isCitySelectionPending_defaultTrue() {
        assertTrue(LandingCityHelper.isCitySelectionPending(prefs));
    }

    @Test
    public void isCitySelectionPending_afterRunFalse() {
        prefs.saveValue("CityCheckActivity", "run");
        assertFalse(LandingCityHelper.isCitySelectionPending(prefs));
    }
}
