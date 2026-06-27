package com.taxi.easy.ua.utils.auth;

import androidx.annotation.Nullable;

import com.google.firebase.auth.GetTokenResult;

public final class FirebaseConsentTokenHelper {

    static final int NULL_USER_MAX_ATTEMPTS = 4;
    static final int NULL_USER_RETRY_DELAY_MS = 250;

    private FirebaseConsentTokenHelper() {
    }

    public static boolean isNonEmptyToken(@Nullable String token) {
        return token != null && !token.trim().isEmpty();
    }

    @Nullable
    public static String extractToken(@Nullable GetTokenResult result) {
        return result != null ? result.getToken() : null;
    }

    public static boolean shouldRetryForNullUser(int attempt) {
        return attempt < NULL_USER_MAX_ATTEMPTS;
    }
}
