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

    /**
     * Гость только если нет Firebase-сессии и нет реального email в БД/prefs.
     * Phone Auth часто даёт {@code getEmail() == null} — это всё равно вход.
     */
    public static boolean isGuestSession(boolean hasFirebaseUser,
                                         @Nullable String dbEmail,
                                         @Nullable String prefsEmail) {
        if (hasFirebaseUser) {
            return false;
        }
        return isGuestEmail(dbEmail) && isGuestEmail(prefsEmail);
    }
}
