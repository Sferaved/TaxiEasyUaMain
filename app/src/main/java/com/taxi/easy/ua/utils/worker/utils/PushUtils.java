package com.taxi.easy.ua.utils.worker.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.log.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PushUtils {
    public static void insertPushDate(Context context) {
        try (SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null)) {
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MainActivity.TABLE_LAST_PUSH +
                    " (push_date TEXT PRIMARY KEY)");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());
            Logger.d(context, "InsertPushDate", "Current date and time: " + currentDateandTime);

            ContentValues values = new ContentValues();
            values.put("push_date", currentDateandTime);

            long rowId = database.insertWithOnConflict(MainActivity.TABLE_LAST_PUSH, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);

            if (rowId != -1) {
                Logger.d(context, "InsertPushDate", "Insert or update successful");
            } else {
                Logger.d(context, "InsertPushDate", "Error inserting or updating");
            }
        } catch (Exception e) {
            Logger.e(context, "InsertPushDate", "Error in insertPushDate: " + e.getMessage());
        }
    }
}

