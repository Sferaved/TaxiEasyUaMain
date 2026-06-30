package com.taxi.easy.ua.utils.inclusive;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.location.AutoLocationAfterCityHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.InclusiveTransportPreferenceWorker;

import java.util.Locale;

/**
 * Shows inclusive transport dialog once per user email after successful Firebase auth,
 * when the order screen is ready (city selected).
 */
public final class InclusiveTransportPromptCoordinator {

    private static final String TAG = "InclusiveTransportPrompt";
    private static final String CITY_CHECK_KEY = "CityCheckActivity";
    private static final String CITY_NOT_SELECTED = "**";

    private InclusiveTransportPromptCoordinator() {
    }

    /** Call after Firebase sign-in succeeds (optional; order screen resume also triggers show). */
    public static void onAuthSucceeded() {
        Logger.d(null, TAG, "auth succeeded — inclusive prompt on order screen when ready");
    }

    /** Call from order screen when UI is ready (VisicomFragment.onResume, after location flow). */
    public static void tryShowOnMapReady(MainActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (activity.isBlockingInclusiveTransportPrompt()) {
            return;
        }
        if (activity.isInclusiveTransportDialogShowing()) {
            return;
        }
        if (InclusiveTransportPreferenceWorker.hasBeenAskedForCurrentUser()) {
            return;
        }
        if (!isCitySelectionComplete()) {
            return;
        }
        if (shouldDeferForLocationFlow(activity)) {
            Logger.d(activity, TAG, "defer inclusive prompt — location flow in progress");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(resolveEmail(user))) {
            return;
        }

        Logger.d(activity, TAG, "showing inclusive transport dialog after auth");
        activity.showInclusiveTransportDialog();
    }

    /**
     * Wait until auto-location after city selection finishes (permission dialog / GPS),
     * so inclusive transport prompt does not overlap the system location request.
     */
    public static boolean shouldDeferForLocationFlow(Context context) {
        if (context == null || !isCitySelectionComplete()) {
            return false;
        }
        if (AutoLocationAfterCityHelper.hasLocationPermission(context)) {
            return false;
        }
        return AutoLocationAfterCityHelper.isPending();
    }

    private static boolean isCitySelectionComplete() {
        Object value = sharedPreferencesHelperMain.getValue(CITY_CHECK_KEY, CITY_NOT_SELECTED);
        String cityCheck = value == null ? CITY_NOT_SELECTED : String.valueOf(value);
        return !CITY_NOT_SELECTED.equals(cityCheck);
    }

    private static String resolveEmail(FirebaseUser user) {
        String email = user.getEmail();
        if (!TextUtils.isEmpty(email)) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        Object stored = sharedPreferencesHelperMain.getValue("userEmail", "");
        return stored == null ? "" : String.valueOf(stored).trim().toLowerCase(Locale.ROOT);
    }
}
