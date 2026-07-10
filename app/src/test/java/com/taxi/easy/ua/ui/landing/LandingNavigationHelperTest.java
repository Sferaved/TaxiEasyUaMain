package com.taxi.easy.ua.ui.landing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LandingNavigationHelperTest {

    @Test
    public void requiresAuth_operatorDriverLanguageFalse_otherTrue() {
        assertFalse(LandingNavigationHelper.requiresAuth(LandingAction.OPERATOR));
        assertFalse(LandingNavigationHelper.requiresAuth(LandingAction.DRIVER));
        assertFalse(LandingNavigationHelper.requiresAuth(LandingAction.LANGUAGE));
        assertTrue(LandingNavigationHelper.requiresAuth(LandingAction.ORDER));
        assertTrue(LandingNavigationHelper.requiresAuth(LandingAction.CITY));
        assertTrue(LandingNavigationHelper.requiresAuth(null));
    }

    @Test
    public void shouldPromptCityBeforeAction_paymentLanguageUseDefaultCity() {
        assertFalse(LandingNavigationHelper.shouldPromptCityBeforeAction(LandingAction.CITY));
        assertFalse(LandingNavigationHelper.shouldPromptCityBeforeAction(LandingAction.OPERATOR));
        assertFalse(LandingNavigationHelper.shouldPromptCityBeforeAction(LandingAction.DRIVER));
        assertFalse(LandingNavigationHelper.shouldPromptCityBeforeAction(LandingAction.PAYMENT));
        assertFalse(LandingNavigationHelper.shouldPromptCityBeforeAction(LandingAction.LANGUAGE));
        assertTrue(LandingNavigationHelper.shouldPromptCityBeforeAction(LandingAction.ORDER));
        assertFalse(LandingNavigationHelper.shouldPromptCityBeforeAction(null));
    }

    @Test
    public void shouldLaunchCityCheckAfterAuth_onlyOrderAndCalculation() {
        assertFalse(LandingNavigationHelper.shouldLaunchCityCheckAfterAuth(LandingAction.CITY));
        assertFalse(LandingNavigationHelper.shouldLaunchCityCheckAfterAuth(LandingAction.PAYMENT));
        assertFalse(LandingNavigationHelper.shouldLaunchCityCheckAfterAuth(LandingAction.LANGUAGE));
        assertFalse(LandingNavigationHelper.shouldLaunchCityCheckAfterAuth(null));
        assertTrue(LandingNavigationHelper.shouldLaunchCityCheckAfterAuth(LandingAction.ORDER));
        assertTrue(LandingNavigationHelper.shouldLaunchCityCheckAfterAuth(LandingAction.CALCULATION));
    }

    @Test
    public void appliesDefaultCityInsteadOfPicker_paymentAndLanguageOnly() {
        assertTrue(LandingNavigationHelper.appliesDefaultCityInsteadOfPicker(LandingAction.PAYMENT));
        assertTrue(LandingNavigationHelper.appliesDefaultCityInsteadOfPicker(LandingAction.LANGUAGE));
        assertFalse(LandingNavigationHelper.appliesDefaultCityInsteadOfPicker(LandingAction.ORDER));
    }
}
