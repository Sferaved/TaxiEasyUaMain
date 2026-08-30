package com.taxi.easy.ua.utils.city;

/**
 * UI after the user declines a GPS city-change prompt (Mantis #58).
 * Keep current start/finish and show order buttons; do not wipe the order screen.
 */
public final class CityChangeDeclineUiHelper {

    private CityChangeDeclineUiHelper() {
    }

    public static boolean shouldClearAddressFields() {
        return false;
    }

    public static boolean shouldShowOrderButtons() {
        return true;
    }
}
