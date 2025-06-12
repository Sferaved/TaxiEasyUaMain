package com.taxi.easy.ua.utils.worker.utils;


import android.content.Context;
import android.content.SharedPreferences;

import com.taxi.easy.ua.MainActivity;

public class VersionUtils {

    static final String PREFS_NAME_VERSION = "MyPrefsFileNew";
    private static final String LAST_NOTIFICATION_TIME_KEY = "lastNotificationTimeNew";
    public static void versionFromMarket(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME_VERSION, Context.MODE_PRIVATE);
        long lastNotificationTime = sharedPreferences.getLong(LAST_NOTIFICATION_TIME_KEY, 0);
        long currentTime = System.currentTimeMillis();
        long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

        if (currentTime - lastNotificationTime >= ONE_DAY_IN_MILLISECONDS) {
            MainActivity.checkForUpdateForPush(sharedPreferences, currentTime, LAST_NOTIFICATION_TIME_KEY);
        }
    }


}
