package com.taxi.easy.ua.utils.worker;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public final class InclusiveTransportPreferenceWorker {

    private static final String KEY_INCLUSIVE_TRANSPORT_ASKED = "inclusive_transport_asked";
    private static final String KEY_INCLUSIVE_TRANSPORT_ENABLED = "inclusive_transport_enabled";
    private static final String KEY_ASKED_PREFIX = "inclusive_transport_asked_";
    private static final String KEY_ENABLED_PREFIX = "inclusive_transport_enabled_";

    private InclusiveTransportPreferenceWorker() {
    }

    public static void saveUserPreference(boolean needsInclusiveTransport) {
        String email = resolveCurrentUserEmail();
        if (!email.isEmpty()) {
            sharedPreferencesHelperMain.saveValue(enabledKey(email), needsInclusiveTransport);
            sharedPreferencesHelperMain.saveValue(askedKey(email), true);
        } else {
            sharedPreferencesHelperMain.saveValue(KEY_INCLUSIVE_TRANSPORT_ENABLED, needsInclusiveTransport);
            sharedPreferencesHelperMain.saveValue(KEY_INCLUSIVE_TRANSPORT_ASKED, true);
        }
    }

    public static boolean needsInclusiveTransport() {
        String email = resolveCurrentUserEmail();
        if (!email.isEmpty()) {
            return (boolean) sharedPreferencesHelperMain.getValue(enabledKey(email), false);
        }
        return (boolean) sharedPreferencesHelperMain.getValue(KEY_INCLUSIVE_TRANSPORT_ENABLED, false);
    }

    /** @deprecated use {@link #hasBeenAskedForCurrentUser()} */
    @Deprecated
    public static boolean hasBeenAsked() {
        return hasBeenAskedForCurrentUser();
    }

    public static boolean hasBeenAskedForCurrentUser() {
        String email = resolveCurrentUserEmail();
        if (!email.isEmpty()) {
            return (boolean) sharedPreferencesHelperMain.getValue(askedKey(email), false);
        }
        return (boolean) sharedPreferencesHelperMain.getValue(KEY_INCLUSIVE_TRANSPORT_ASKED, false);
    }

    static String askedKey(String normalizedEmail) {
        return KEY_ASKED_PREFIX + normalizedEmail;
    }

    static String enabledKey(String normalizedEmail) {
        return KEY_ENABLED_PREFIX + normalizedEmail;
    }

    static String resolveCurrentUserEmail() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && !TextUtils.isEmpty(user.getEmail())) {
                return user.getEmail().trim().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalStateException ignored) {
            // Firebase not initialized in unit tests
        }
        Object stored = sharedPreferencesHelperMain.getValue("userEmail", "");
        if (stored == null) {
            return "";
        }
        String email = String.valueOf(stored).trim();
        return email.isEmpty() ? "" : email.toLowerCase(Locale.ROOT);
    }
}
