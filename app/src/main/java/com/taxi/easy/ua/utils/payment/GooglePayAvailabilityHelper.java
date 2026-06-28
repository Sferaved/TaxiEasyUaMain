package com.taxi.easy.ua.utils.payment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.db.CursorReadHelper;
import com.taxi.easy.ua.utils.permissions.UserPermissions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Синхронная проверка: можно ли предложить Google Pay (город + права пользователя). */
public final class GooglePayAvailabilityHelper {

    private static final Set<String> BLOCKED_CITIES = new HashSet<>(Arrays.asList(
            "foreign countries",
            "Dnipropetrovsk Oblast",
            "Odessa",
            "Zaporizhzhia",
            "Cherkasy Oblast"
    ));

    private GooglePayAvailabilityHelper() {
    }

    public static boolean isGooglePayOfferAvailable(@NonNull Context context) {
        String city = readCity(context);
        if (BLOCKED_CITIES.contains(city)) {
            return false;
        }
        String[] permissions = UserPermissions.getUserPayPermissions(context);
        return permissions.length > 1 && !"0".equals(permissions[1]);
    }

    @NonNull
    private static String readCity(@NonNull Context context) {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
            cursor = database.query(
                    MainActivity.CITY_INFO,
                    new String[]{"city"},
                    "id = ?",
                    new String[]{"1"},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex("city");
                if (index >= 0) {
                    String city = CursorReadHelper.getString(cursor, "city");
                    return city != null ? city : "";
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
        return "";
    }
}
