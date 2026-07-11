package com.taxi.easy.ua.utils.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GuestSessionHelperTest {

    @Test
    public void isGuestEmail_placeholderEmail_returnsTrue() {
        assertTrue(GuestSessionHelper.isGuestEmail("email"));
        assertTrue(GuestSessionHelper.isGuestEmail("no_email"));
    }

    @Test
    public void isGuestEmail_emptyOrNull_returnsTrue() {
        assertTrue(GuestSessionHelper.isGuestEmail(null));
        assertTrue(GuestSessionHelper.isGuestEmail(""));
        assertTrue(GuestSessionHelper.isGuestEmail("   "));
    }

    @Test
    public void isGuestEmail_realEmail_returnsFalse() {
        assertFalse(GuestSessionHelper.isGuestEmail("user@example.com"));
    }

    @Test
    public void isGuestSession_firebaseUserPresent_notGuestEvenWithoutEmail() {
        assertFalse(GuestSessionHelper.isGuestSession(true, "email", "no_email"));
        assertFalse(GuestSessionHelper.isGuestSession(true, null, null));
    }

    @Test
    public void isGuestSession_noFirebase_realDbOrPrefs_notGuest() {
        assertFalse(GuestSessionHelper.isGuestSession(false, "user@example.com", "no_email"));
        assertFalse(GuestSessionHelper.isGuestSession(false, "email", "user@example.com"));
    }

    @Test
    public void isGuestSession_noFirebase_placeholderOnly_isGuest() {
        assertTrue(GuestSessionHelper.isGuestSession(false, "email", "no_email"));
        assertTrue(GuestSessionHelper.isGuestSession(false, null, null));
    }
}
