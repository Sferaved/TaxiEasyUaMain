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
}
