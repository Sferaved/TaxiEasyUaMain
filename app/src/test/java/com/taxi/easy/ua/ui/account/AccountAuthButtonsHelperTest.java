package com.taxi.easy.ua.ui.account;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccountAuthButtonsHelperTest {

    @Test
    public void guest_alwaysShowsLogin_evenOffline() {
        assertTrue(AccountAuthButtonsHelper.shouldShowLoginForGuest(true));
        assertTrue(AccountAuthButtonsHelper.shouldShowLoginForGuest(false));
    }

    @Test
    public void loggedIn_alwaysShowsLogout() {
        assertTrue(AccountAuthButtonsHelper.shouldShowLogoutWhenLoggedIn());
    }

    @Test
    public void cancelRoutes_failedOrEmpty_keepsLogout() {
        assertTrue(AccountAuthButtonsHelper.shouldShowLogoutAfterCancelRoutes(true, false));
        assertTrue(AccountAuthButtonsHelper.shouldShowLogoutAfterCancelRoutes(true, true));
    }

    @Test
    public void cancelRoutes_asterisk_showsLogout() {
        assertTrue(AccountAuthButtonsHelper.shouldShowLogoutAfterCancelRoutes(false, true));
    }

    @Test
    public void cancelRoutes_pendingOnly_noLogoutFromHelper() {
        assertFalse(AccountAuthButtonsHelper.shouldShowLogoutAfterCancelRoutes(false, false));
    }
}
