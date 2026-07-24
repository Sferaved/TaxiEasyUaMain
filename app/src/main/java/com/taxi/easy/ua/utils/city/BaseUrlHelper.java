package com.taxi.easy.ua.utils.city;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

/**
 * Базовый URL API из Firestore:
 * <ul>
 *   <li>{@code keys/base_urls} — поля {@code prod}, {@code test} (глобальные дефолты)</li>
 *   <li>{@code city/{city}.base_url} — URL выбранного города (live)</li>
 * </ul>
 * В коде нет захардкоженных хостов — только кэш в SharedPreferences.
 */
public final class BaseUrlHelper {

    public static final String TAG = "BaseUrlHelper";
    public static final String PREF_KEY = "baseUrl";
    public static final String PREF_KEY_PROD = "baseUrl_prod";
    public static final String PREF_KEY_TEST = "baseUrl_test";
    /** Город, для которого берётся test-дефолт из keys/base_urls. */
    public static final String TEST_CITY = "OdessaTest";

    private BaseUrlHelper() {
    }

    /** Сохранить prod/test из {@code keys/base_urls}. */
    public static void applyGlobalDefaults(@Nullable SharedPreferencesHelper prefs,
                                           @Nullable String prod,
                                           @Nullable String test) {
        if (prefs == null) {
            return;
        }
        String nProd = normalize(prod);
        String nTest = normalize(test);
        if (isValidHttpUrl(nProd)) {
            String prev = readPref(prefs, PREF_KEY_PROD);
            prefs.saveValue(PREF_KEY_PROD, nProd);
            if (!nProd.equals(prev)) {
                String msg = "baseUrl defaults CHANGED [prod]: " + prev + " -> " + nProd;
                Log.i(TAG, msg);
                logToAppFile(msg);
            }
            // Первый запуск: пока город не выбран — seed активного URL из prod
            if (validOrNull(readPref(prefs, PREF_KEY)) == null) {
                applyToPrefs(prefs, nProd, "defaults-seed", null);
            }
        } else {
            Log.w(TAG, "ignore invalid prod default: " + prod);
        }
        if (isValidHttpUrl(nTest)) {
            String prev = readPref(prefs, PREF_KEY_TEST);
            prefs.saveValue(PREF_KEY_TEST, nTest);
            if (!nTest.equals(prev)) {
                String msg = "baseUrl defaults CHANGED [test]: " + prev + " -> " + nTest;
                Log.i(TAG, msg);
                logToAppFile(msg);
            }
        } else {
            Log.w(TAG, "ignore invalid test default: " + test);
        }
    }

    @Nullable
    public static String prodFromPrefs(@Nullable SharedPreferencesHelper prefs) {
        return validOrNull(readPref(prefs, PREF_KEY_PROD));
    }

    @Nullable
    public static String testFromPrefs(@Nullable SharedPreferencesHelper prefs) {
        return validOrNull(readPref(prefs, PREF_KEY_TEST));
    }

    /**
     * Fallback до прихода city.base_url: test-город → keys/base_urls.test, иначе prod.
     * Может вернуть null, если глобальные дефолты ещё не загружены.
     */
    @Nullable
    public static String fallbackForCity(@Nullable SharedPreferencesHelper prefs,
                                         @Nullable String city) {
        if (TEST_CITY.equals(city)) {
            return testFromPrefs(prefs);
        }
        return prodFromPrefs(prefs);
    }

    @Nullable
    public static String normalize(@Nullable String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean isValidHttpUrl(@Nullable String url) {
        String normalized = normalize(url);
        return normalized != null
                && (normalized.startsWith("https://") || normalized.startsWith("http://"));
    }

    @NonNull
    public static String documentIdForCity(@Nullable String city) {
        if (isKnownCity(city)) {
            return city;
        }
        return "foreign countries";
    }

    public static boolean isKnownCity(@Nullable String city) {
        if (city == null || city.isEmpty()) {
            return false;
        }
        switch (city) {
            case "Kyiv City":
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
            case "Lviv":
            case "Ivano_frankivsk":
            case "Vinnytsia":
            case "Poltava":
            case "Sumy":
            case "Kharkiv":
            case "Chernihiv":
            case "Rivne":
            case "Ternopil":
            case "Khmelnytskyi":
            case "Zakarpattya":
            case "Zhytomyr":
            case "Kropyvnytskyi":
            case "Mykolaiv":
            case "Chernivtsi":
            case "Lutsk":
            case "OdessaTest":
            case "foreign countries":
                return true;
            default:
                return false;
        }
    }

    public static void applyToPrefs(@Nullable SharedPreferencesHelper prefs, @Nullable String url) {
        applyToPrefs(prefs, url, "update", null);
    }

    public static void applyToPrefs(@Nullable SharedPreferencesHelper prefs,
                                    @Nullable String url,
                                    @NonNull String reason,
                                    @Nullable String city) {
        if (prefs == null) {
            return;
        }
        String normalized = normalize(url);
        if (!isValidHttpUrl(normalized)) {
            Log.w(TAG, "ignore invalid baseUrl [" + reason + "] city=" + city + " raw=" + url);
            return;
        }
        String previous = peekPrefs(prefs);
        prefs.saveValue(PREF_KEY, normalized);
        if (!normalized.equals(previous)) {
            String msg = "baseUrl CHANGED [" + reason + "] city=" + city
                    + ": " + previous + " -> " + normalized;
            Log.i(TAG, msg);
            logToAppFile(msg);
        } else {
            Log.d(TAG, "baseUrl same [" + reason + "] city=" + city + ": " + normalized);
        }
    }

    public static void applyFallback(@Nullable SharedPreferencesHelper prefs, @Nullable String city) {
        String docCity = documentIdForCity(city);
        String fallback = fallbackForCity(prefs, docCity);
        if (!isValidHttpUrl(fallback)) {
            Log.w(TAG, "no global defaults yet for city=" + docCity
                    + " (wait keys/base_urls); keep current=" + peekPrefs(prefs));
            return;
        }
        applyToPrefs(prefs, fallback, "fallback", docCity);
    }

    @NonNull
    private static String peekPrefs(@Nullable SharedPreferencesHelper prefs) {
        String current = validOrNull(readPref(prefs, PREF_KEY));
        if (current != null) {
            return current;
        }
        return "(empty)";
    }

    /**
     * Активный baseUrl: текущий город → иначе prod из keys/base_urls → иначе "".
     */
    @NonNull
    public static String fromPrefs(@Nullable SharedPreferencesHelper prefs) {
        String current = validOrNull(readPref(prefs, PREF_KEY));
        if (current != null) {
            return current;
        }
        String prod = prodFromPrefs(prefs);
        if (prod != null) {
            return prod;
        }
        Log.w(TAG, "fromPrefs: no baseUrl and no prod default yet");
        return "";
    }

    @NonNull
    public static String fromPrefsWithSlash(@Nullable SharedPreferencesHelper prefs) {
        String base = fromPrefs(prefs);
        if (base.isEmpty()) {
            return "";
        }
        return base.endsWith("/") ? base : base + "/";
    }

    /** Centrifugo websocket: https://host → wss://host/connection/websocket */
    @NonNull
    public static String centrifugoWsUrl(@Nullable SharedPreferencesHelper prefs) {
        String https = fromPrefs(prefs);
        if (https.isEmpty()) {
            Log.w(TAG, "centrifugoWsUrl: empty baseUrl");
            return "";
        }
        String ws = https.startsWith("https://")
                ? "wss://" + https.substring("https://".length())
                : https.startsWith("http://")
                ? "ws://" + https.substring("http://".length())
                : "wss://" + https;
        return ws + "/connection/websocket";
    }

    /**
     * Слушает keys/base_urls, затем city/{city}.base_url.
     */
    public static void syncForCity(@Nullable Context context,
                                   @Nullable String city,
                                   @Nullable SharedPreferencesHelper prefs) {
        String docCity = documentIdForCity(city);
        Log.i(TAG, "syncForCity start city=" + city + " doc=" + docCity);
        applyFallback(prefs, docCity);
        if (context == null || prefs == null) {
            return;
        }
        FirestoreHelper helper = resolveFirestoreHelper(context);
        if (helper == null) {
            Log.w(TAG, "syncForCity: no FirestoreHelper, keep fallback");
            return;
        }
        helper.listenBaseUrlForCity(docCity, new FirestoreHelper.OnBaseUrlFetchedListener() {
            @Override
            public void onSuccess(String baseUrl) {
                applyToPrefs(prefs, baseUrl, "firestore", docCity);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "firestore city base_url miss city=" + docCity
                        + " keep=" + fromPrefs(prefs)
                        + " err=" + (e != null ? e.getMessage() : "null"));
            }
        });
    }

    /** Live-слушатель глобальных prod/test. */
    public static void syncGlobalDefaults(@Nullable Context context,
                                          @Nullable SharedPreferencesHelper prefs) {
        if (context == null || prefs == null) {
            return;
        }
        FirestoreHelper helper = resolveFirestoreHelper(context);
        if (helper == null) {
            return;
        }
        helper.listenBaseUrlDefaults(new FirestoreHelper.OnBaseUrlDefaultsFetchedListener() {
            @Override
            public void onSuccess(String prod, String test) {
                applyGlobalDefaults(prefs, prod, test);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "keys/base_urls miss: " + (e != null ? e.getMessage() : "null"));
            }
        });
    }

    @Nullable
    private static String readPref(@Nullable SharedPreferencesHelper prefs, @NonNull String key) {
        if (prefs == null) {
            return null;
        }
        Object value = prefs.getValue(key, "");
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    @Nullable
    private static String validOrNull(@Nullable String url) {
        String n = normalize(url);
        return isValidHttpUrl(n) ? n : null;
    }

    private static void logToAppFile(@NonNull String msg) {
        try {
            Context ctx = MyApplication.getContext();
            if (ctx != null) {
                Logger.d(ctx, TAG, msg);
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    @Nullable
    private static FirestoreHelper resolveFirestoreHelper(@NonNull Context context) {
        MyApplication app = MyApplication.getInstance();
        if (app != null && app.getFirestoreHelper() != null) {
            return app.getFirestoreHelper();
        }
        return new FirestoreHelper(context.getApplicationContext());
    }
}
