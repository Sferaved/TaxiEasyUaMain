package com.taxi.easy.ua.utils.analytics;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.utils.cost.CostParseHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * События Firebase Analytics для импорта конверсий в Google Ads:
 * first_open (авто), sign_up, purchase (заказ такси).
 */
public final class AdsConversionHelper {

    private static final String TAG = "AdsConversionHelper";
    private static final String PREF_SIGN_UP_PREFIX = "ads_sign_up_logged_";
    private static final long NEW_USER_WINDOW_MS = 60_000L;

    private static final Set<String> LOGGED_ORDER_UIDS =
            Collections.synchronizedSet(new HashSet<>());

    private AdsConversionHelper() {
    }

    public static void logSignUpIfNewUser(Context context, @Nullable FirebaseUser user) {
        if (context == null || user == null || TextUtils.isEmpty(user.getEmail())) {
            return;
        }
        if (!isNewFirebaseUser(user)) {
            return;
        }

        String emailKey = PREF_SIGN_UP_PREFIX + user.getEmail().trim().toLowerCase(Locale.US);
        boolean alreadyLogged = context.getSharedPreferences("ads_conversions", Context.MODE_PRIVATE)
                .getBoolean(emailKey, false);
        if (alreadyLogged) {
            return;
        }

        try {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            analytics.setUserId(user.getUid());

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.METHOD, "google");
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, params);

            context.getSharedPreferences("ads_conversions", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(emailKey, true)
                    .apply();

            Logger.d(context, TAG, "sign_up logged for " + user.getEmail());
        } catch (Exception e) {
            Logger.e(context, TAG, "sign_up failed: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void logOrderPlaced(
            Context context,
            @Nullable String orderUid,
            @Nullable String orderCost,
            @Nullable String currency,
            @Nullable String payMethod
    ) {
        if (context == null || TextUtils.isEmpty(orderUid)) {
            return;
        }
        if (!CostParseHelper.hasDisplayableCost(orderCost)) {
            return;
        }
        if (LOGGED_ORDER_UIDS.contains(orderUid)) {
            return;
        }
        LOGGED_ORDER_UIDS.add(orderUid);

        double value = parseAnalyticsValue(orderCost);
        if (value <= 0) {
            return;
        }

        try {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.TRANSACTION_ID, orderUid);
            params.putString(FirebaseAnalytics.Param.CURRENCY, normalizeCurrency(currency));
            params.putDouble(FirebaseAnalytics.Param.VALUE, value);
            if (!TextUtils.isEmpty(payMethod)) {
                params.putString(FirebaseAnalytics.Param.PAYMENT_TYPE, payMethod);
            }
            analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, params);

            Logger.d(context, TAG, "purchase logged uid=" + orderUid + " value=" + value);
        } catch (Exception e) {
            LOGGED_ORDER_UIDS.remove(orderUid);
            Logger.e(context, TAG, "purchase failed: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    static boolean isNewFirebaseUser(@Nullable FirebaseUser user) {
        if (user == null) {
            return false;
        }
        FirebaseUserMetadata metadata = user.getMetadata();
        if (metadata == null) {
            return false;
        }
        long creation = metadata.getCreationTimestamp();
        long lastSignIn = metadata.getLastSignInTimestamp();
        return Math.abs(lastSignIn - creation) <= NEW_USER_WINDOW_MS;
    }

    static double parseAnalyticsValue(@Nullable String orderCost) {
        if (orderCost == null) {
            return 0d;
        }
        String trimmed = orderCost.trim();
        if (trimmed.isEmpty()) {
            return 0d;
        }
        try {
            double value = Double.parseDouble(trimmed.replace(',', '.'));
            if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                return 0d;
            }
            return value;
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static String normalizeCurrency(@Nullable String currency) {
        if (TextUtils.isEmpty(currency)) {
            return "UAH";
        }
        return currency.trim().toUpperCase(Locale.US);
    }
}
