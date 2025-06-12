package com.taxi.easy.ua.utils.worker.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.log.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PushDateUpdater {
    private static final String TAG = "PushDateUpdater";

    public static void updatePushDate(Context context) {
        SQLiteDatabase database = null;
        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);

            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());

            Logger.d(context, TAG, "Current date and time: " + currentDateandTime);

            ContentValues values = new ContentValues();
            values.put("push_date", currentDateandTime);

            int rowsAffected = database.update(MainActivity.TABLE_LAST_PUSH, values, "ROWID=1", null);
            if (rowsAffected > 0) {
                Logger.d(context, TAG, "Update successful");
            } else {
                Logger.d(context, TAG, "Error updating");
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }
}
