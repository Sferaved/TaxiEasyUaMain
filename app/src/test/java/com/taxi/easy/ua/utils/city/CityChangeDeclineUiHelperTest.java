package com.taxi.easy.ua.utils.city;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Mantis #58: declining «Інше» after overseas GPS must not empty the order screen.
 */
public class CityChangeDeclineUiHelperTest {

    @Test
    public void decline_doesNotClearStartAndFinish() {
        assertFalse(CityChangeDeclineUiHelper.shouldClearAddressFields());
    }

    @Test
    public void decline_keepsOrderButtonsVisible() {
        assertTrue(CityChangeDeclineUiHelper.shouldShowOrderButtons());
    }
}
