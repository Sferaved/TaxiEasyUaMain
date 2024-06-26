package com.taxi.easy.ua.utils.notif;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import java.util.List;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    public static void logNotificationChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();
            for (NotificationChannel channel : channels) {
                Log.d(TAG, "Channel ID: " + channel.getId() + ", Name: " + channel.getName());
            }
        }
    }

    public static void disableNotificationChannel(Context context, String channelId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.deleteNotificationChannel(channelId);
            NotificationChannel newChannel = new NotificationChannel(channelId, "Foreground Service Channel", NotificationManager.IMPORTANCE_NONE);
            newChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
            newChannel.setSound(null, null);
            newChannel.enableVibration(false);
            notificationManager.createNotificationChannel(newChannel);
        }
    }
}

