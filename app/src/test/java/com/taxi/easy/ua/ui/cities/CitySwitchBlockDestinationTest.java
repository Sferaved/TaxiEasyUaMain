package com.taxi.easy.ua.ui.cities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CitySwitchBlockDestinationTest {

    @Test
    public void blocksPaymentMethodsAndActiveOrderScreens() {
        assertTrue(MainActivity.isCitySwitchBlockedDestination(R.id.nav_card));
        assertTrue(MainActivity.isCitySwitchBlockedDestination(R.id.nav_finish_separate));
        assertTrue(MainActivity.isCitySwitchBlockedDestination(R.id.nav_card_ver));
    }

    @Test
    public void allowsCitySwitchOnOrderMap() {
        assertFalse(MainActivity.isCitySwitchBlockedDestination(R.id.nav_visicom));
        assertFalse(MainActivity.isCitySwitchBlockedDestination(R.id.nav_city));
    }
}
