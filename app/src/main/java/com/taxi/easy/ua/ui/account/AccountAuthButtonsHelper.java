package com.taxi.easy.ua.ui.account;

/**
 * Visibility rules for account login/logout buttons.
 */
public final class AccountAuthButtonsHelper {

    private AccountAuthButtonsHelper() {
    }

    /** Guest: login should stay visible even if network check flaps (Wi‑Fi/4G). */
    public static boolean shouldShowLoginForGuest(boolean networkAvailable) {
        return true;
    }

    /** Logged-in: logout immediately, without waiting for cancel-routes API. */
    public static boolean shouldShowLogoutWhenLoggedIn() {
        return true;
    }

    /**
     * After cancel-routes API:
     * - free account (* or empty/failed) → logout+delete OK
     * - scheduled cancels → hide (handled elsewhere)
     */
    public static boolean shouldShowLogoutAfterCancelRoutes(
            boolean requestFailedOrEmpty,
            boolean accountFreeAsterisk) {
        return requestFailedOrEmpty || accountFreeAsterisk;
    }
}
