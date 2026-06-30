package com.taxi.easy.ua.utils.inclusive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.location.AutoLocationAfterCityHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class InclusiveTransportPromptCoordinatorTest {

    @Before
    public void setUp() {
        MyApplication.sharedPreferencesHelperMain =
                new com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper(RuntimeEnvironment.getApplication());
    }

    @Test
    public void shouldDeferForLocationFlow_whenPendingAndNoPermission() {
        MyApplication.sharedPreferencesHelperMain.saveValue("CityCheckActivity", "run");
        MyApplication.sharedPreferencesHelperMain.saveValue(
                AutoLocationAfterCityHelper.KEY_PENDING_AUTO_LOCATION, true);

        assertTrue(InclusiveTransportPromptCoordinator.shouldDeferForLocationFlow(
                RuntimeEnvironment.getApplication()));
    }

    @Test
    public void shouldNotDeferForLocationFlow_whenNotPending() {
        MyApplication.sharedPreferencesHelperMain.saveValue("CityCheckActivity", "run");
        MyApplication.sharedPreferencesHelperMain.saveValue(
                AutoLocationAfterCityHelper.KEY_PENDING_AUTO_LOCATION, false);

        assertFalse(InclusiveTransportPromptCoordinator.shouldDeferForLocationFlow(
                RuntimeEnvironment.getApplication()));
    }
}
