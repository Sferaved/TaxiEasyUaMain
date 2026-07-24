package com.taxi.easy.ua.utils.location;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.city.BaseUrlHelper;
import com.taxi.easy.ua.utils.db.CursorReadHelper;
import com.taxi.easy.ua.utils.from_json_parser.FromJSONParserRetrofit;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Обратный геокод GPS-координат через тот же endpoint, что и экран заказа.
 */
public final class GpsGeocodeHelper {

    private static final String TAG = "GpsGeocodeHelper";

    private GpsGeocodeHelper() {
    }

    public static void reverseGeocode(Context context, double latitude, double longitude,
                                      Consumer<String> onAddress) {
        reverseGeocode(context, latitude, longitude, false, onAddress);
    }

    private static void reverseGeocode(Context context, double latitude, double longitude,
                                       boolean productionRetry, Consumer<String> onAddress) {
        if (context == null) {
            onAddress.accept(null);
            return;
        }

        List<String> cityInfo = readCityInfo(context);
        String api = cityInfo.size() > 2 ? cityInfo.get(2) : MainActivity.api;
        String city = cityInfo.size() > 1 ? cityInfo.get(1) : "";
        String language = LocaleHelper.getLocale();
        String baseUrl = BaseUrlHelper.fromPrefs(sharedPreferencesHelperMain);
        String url = baseUrl + "/" + api + "/android/fromSearchGeoLocal/"
                + latitude + "/" + longitude + "/" + language;
        Logger.d(context, TAG, "reverseGeocode: " + url);

        FromJSONParserRetrofit.sendURL(url, result -> {
            String address = parseAddress(context, result);
            if (!productionRetry
                    && isPlaceholderAddress(context, address)
                    && shouldRetryOnProductionServer(city, baseUrl)) {
                Logger.d(context, TAG, "reverseGeocode: заглушка на тестовом сервере — повтор на prod");
                String prod = BaseUrlHelper.prodFromPrefs(sharedPreferencesHelperMain);
                if (prod != null) {
                    BaseUrlHelper.applyToPrefs(sharedPreferencesHelperMain, prod, "gps-prod-retry", city);
                    reverseGeocode(context, latitude, longitude, true, onAddress);
                } else {
                    Logger.w(context, TAG, "prod default missing — cannot retry on prod");
                    onAddress.accept(address);
                }
                return;
            }
            onAddress.accept(address);
        });
    }

    public static String parseAddress(Context context, Map<String, String> result) {
        if (result == null) {
            return null;
        }
        String raw = result.get("route_address_from");
        if (raw != null) {
            raw = raw.trim();
        }
        return normalizeServerAddress(context, raw);
    }

    public static String normalizeServerAddress(Context context, String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (raw.contains("Точка на карте")) {
            return context.getString(R.string.startPoint);
        }
        return raw;
    }

    public static boolean isPlaceholderAddress(Context context, String address) {
        if (address == null || address.trim().isEmpty()) {
            return true;
        }
        if (address.contains("Точка на карте")) {
            return true;
        }
        return address.equals(context.getString(R.string.startPoint));
    }

    private static boolean shouldRetryOnProductionServer(String city, String currentBaseUrl) {
        if (BaseUrlHelper.TEST_CITY.equals(city) || currentBaseUrl == null) {
            return false;
        }
        String test = BaseUrlHelper.testFromPrefs(sharedPreferencesHelperMain);
        String normalized = BaseUrlHelper.normalize(currentBaseUrl);
        return test != null && test.equals(normalized);
    }

    private static List<String> readCityInfo(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
        try (Cursor cursor = database.query(MainActivity.CITY_INFO, null, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                return List.of(
                        CursorReadHelper.getString(cursor, "id"),
                        CursorReadHelper.getString(cursor, "city"),
                        CursorReadHelper.getString(cursor, "api"),
                        CursorReadHelper.getString(cursor, "phone"),
                        CursorReadHelper.getString(cursor, "card_max_pay"),
                        CursorReadHelper.getString(cursor, "bonus_max_pay"),
                        CursorReadHelper.getString(cursor, "merchant_fondy"),
                        CursorReadHelper.getString(cursor, "fondy_key_storage")
                );
            }
        } finally {
            database.close();
        }
        return List.of("1", "Kyiv City", MainActivity.api, "", "", "", "", "");
    }
}
