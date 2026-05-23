package com.taxi.easy.ua.utils.location;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * Флаги для авто-геолокации после выбора / загрузки города.
 */
public final class AutoLocationAfterCityHelper {

    public static final String KEY_PENDING_AUTO_LOCATION = "pending_auto_location_after_city";
    public static final String KEY_LOCATION_PROMPT_AFTER_CITY_DONE = "location_permission_prompt_after_city_done";
    public static final String KEY_LOCATION_EVER_GRANTED = "location_permission_ever_granted";

    private AutoLocationAfterCityHelper() {
    }

    public static void markCityLoaded() {
        sharedPreferencesHelperMain.saveValue(KEY_PENDING_AUTO_LOCATION, true);
        sharedPreferencesHelperMain.saveValue("setStatusX", false);
    }

    public static boolean isPending() {
        return Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(KEY_PENDING_AUTO_LOCATION, false));
    }

    public static void clearPending() {
        sharedPreferencesHelperMain.saveValue(KEY_PENDING_AUTO_LOCATION, false);
    }

    public static boolean wasPromptShown() {
        return Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(KEY_LOCATION_PROMPT_AFTER_CITY_DONE, false));
    }

    public static void markPromptShown() {
        sharedPreferencesHelperMain.saveValue(KEY_LOCATION_PROMPT_AFTER_CITY_DONE, true);
    }

    public static boolean wasEverGranted() {
        return Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(KEY_LOCATION_EVER_GRANTED, false));
    }

    public static void markEverGranted() {
        sharedPreferencesHelperMain.saveValue(KEY_LOCATION_EVER_GRANTED, true);
    }

    public static boolean isCityReady() {
        return "run".equals(sharedPreferencesHelperMain.getValue("CityCheckActivity", "**"));
    }

    public static boolean hasLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void syncFromSystemPermission(Context context) {
        if (!hasLocationPermission(context)) {
            return;
        }
        if (!wasEverGranted()) {
            markEverGranted();
        }
        if (!wasPromptShown()) {
            markPromptShown();
        }
    }
}
