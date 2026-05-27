package com.taxi.easy.ua.utils.helpers;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.util.Locale;

public final class LocaleHelper {

    public static final String PREF_LOCALE = "locale";
    private static final String PREFS_NAME = "my_prefs";
    private static final String[] LOCALE_CODES = {"en", "ru", "uk"};

    private LocaleHelper() {
    }

    public static String normalizeLocaleCode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "uk";
        }
        String code = raw.split("_")[0].split("-")[0].toLowerCase(Locale.ROOT);
        for (String supported : LOCALE_CODES) {
            if (supported.equals(code)) {
                return code;
            }
        }
        return "uk";
    }

    public static String getLocale() {
        if (sharedPreferencesHelperMain == null) {
            return localeFromAppCompatOrDefault();
        }
        Object stored = sharedPreferencesHelperMain.getValue(PREF_LOCALE, "uk");
        return normalizeLocaleCode(stored != null ? stored.toString() : "uk");
    }

    private static String localeFromAppCompatOrDefault() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty()) {
            Locale locale = appLocales.get(0);
            if (locale != null && !locale.getLanguage().isEmpty()) {
                return normalizeLocaleCode(locale.getLanguage());
            }
        }
        return "uk";
    }

    private static Context prefsStorageContext(Context context) {
        if (context == null) {
            return null;
        }
        Context app = context.getApplicationContext();
        return app != null ? app : context;
    }

    public static String getSavedLocaleCode(Context context) {
        Context storage = prefsStorageContext(context);
        if (storage != null) {
            SharedPreferences prefs = storage.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (prefs.contains(PREF_LOCALE)) {
                return normalizeLocaleCode(prefs.getString(PREF_LOCALE, "uk"));
            }
        }

        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty()) {
            Locale locale = appLocales.get(0);
            if (locale != null && !locale.getLanguage().isEmpty()) {
                return normalizeLocaleCode(locale.getLanguage());
            }
        }

        if (storage == null) {
            return "uk";
        }
        SharedPreferencesHelper helper = new SharedPreferencesHelper(storage);
        Object stored = helper.getValue(PREF_LOCALE, "uk");
        return normalizeLocaleCode(stored != null ? stored.toString() : "uk");
    }

    public static int localeCodeToSpinnerIndex(String localeCode) {
        return switch (normalizeLocaleCode(localeCode)) {
            case "en" -> 0;
            case "ru" -> 1;
            default -> 2;
        };
    }

    public static String spinnerIndexToLocaleCode(int index) {
        if (index < 0 || index >= LOCALE_CODES.length) {
            return "uk";
        }
        return LOCALE_CODES[index];
    }

    public static void persistLocale(Context context, String localeCode) {
        Context storage = prefsStorageContext(context);
        if (storage == null) {
            return;
        }
        String normalized = normalizeLocaleCode(localeCode);
        storage.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_LOCALE, normalized)
                .commit();
    }

    public static void applyAppLocale(Context context) {
        Context storage = prefsStorageContext(context);
        if (storage == null) {
            return;
        }
        applyAppLocale(storage, getSavedLocaleCode(storage));
    }

    public static void applyAppLocale(Context context, String localeCode) {
        String normalized = normalizeLocaleCode(localeCode);
        Locale locale = Locale.forLanguageTag(normalized);
        Locale.setDefault(locale);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale));
    }

    public static void changeLanguage(Activity activity, String localeCode) {
        if (activity == null) {
            return;
        }
        String normalized = normalizeLocaleCode(localeCode);
        persistLocale(activity, normalized);
        applyAppLocale(activity.getApplicationContext(), normalized);

        Intent launch = new Intent(activity, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(launch);
        activity.finishAffinity();
    }

    public static Context wrapContext(Context context) {
        if (context == null) {
            return null;
        }
        String localeCode = getSavedLocaleCode(context);
        Locale locale = Locale.forLanguageTag(localeCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }
}
