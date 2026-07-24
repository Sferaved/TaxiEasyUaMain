package com.taxi.easy.ua.ui.landing;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.city.BaseUrlHelper;
import com.taxi.easy.ua.utils.location.AutoLocationAfterCityHelper;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.taxi.easy.ua.utils.worker.utils.WfpUtils;

/**
 * Тихая установка Киева по умолчанию с лендинга (оплата, язык).
 */
public final class LandingCityHelper {

    public static final String KYIV_CITY_CODE = "Kyiv City";
    public static final String KYIV_PHONE = "tel:0674443804";
    public static final double KYIV_LAT = 50.451107;
    public static final double KYIV_LON = 30.524907;

    private LandingCityHelper() {
    }

    public static boolean isCitySelectionPending(@NonNull SharedPreferencesHelper prefs) {
        return "**".equals(prefs.getValue("CityCheckActivity", "**"));
    }

    public static void applyDefaultKyivCityIfPending(@NonNull Context context,
                                                     @NonNull SharedPreferencesHelper prefs) {
        if (!isCitySelectionPending(prefs)) {
            return;
        }
        applyDefaultKyivCity(context, prefs);
    }

    public static void applyDefaultKyivCity(@NonNull Context context,
                                            @NonNull SharedPreferencesHelper prefs) {
        String cityMenu = context.getString(R.string.city_kyiv);
        String newTitle = context.getString(R.string.menu_city) + " " + cityMenu;

        prefs.saveValue("countryState", "UA");
        BaseUrlHelper.syncForCity(context, KYIV_CITY_CODE, prefs);
        prefs.saveValue("newTitle", newTitle);
        prefs.saveValue("CityCheckActivity", "run");

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME,
                Context.MODE_PRIVATE, null);
        try {
            ContentValues cityValues = new ContentValues();
            cityValues.put("city", KYIV_CITY_CODE);
            cityValues.put("phone", KYIV_PHONE);
            database.update(MainActivity.CITY_INFO, cityValues, "id = ?", new String[]{"1"});

            ContentValues positionValues = new ContentValues();
            positionValues.put("startLat", KYIV_LAT);
            positionValues.put("startLan", KYIV_LON);
            positionValues.put("position", "");
            database.update(MainActivity.TABLE_POSITION_INFO, positionValues, "id = ?",
                    new String[]{"1"});

            ContentValues markerValues = new ContentValues();
            markerValues.put("startLat", KYIV_LAT);
            markerValues.put("startLan", KYIV_LON);
            markerValues.put("to_lat", KYIV_LAT);
            markerValues.put("to_lng", KYIV_LON);
            markerValues.put("finish", "");
            database.update(MainActivity.ROUT_MARKER, markerValues, "id = ?", new String[]{"1"});
        } finally {
            database.close();
        }

        PhoneCallHelper.saveCityPhoneNumber("0674443804");
        AutoLocationAfterCityHelper.markCityLoaded();
        WfpUtils.enqueueCardTokenFetch(context, KYIV_CITY_CODE);
    }
}
