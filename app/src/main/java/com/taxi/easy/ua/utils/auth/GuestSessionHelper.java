package com.taxi.easy.ua.utils.auth;

import androidx.annotation.Nullable;

/**
 * Определяет, что пользователь ещё не прошёл вход (гостевой режим).
 */
public final class GuestSessionHelper {

    private GuestSessionHelper() {
    }

    public static boolean isGuestEmail(@Nullable String email) {
        if (email == null) {
            return true;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() || "email".equals(trimmed) || "no_email".equals(trimmed);
    }
}
