package com.taxi.easy.ua.utils.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.taxi.easy.ua.utils.log.Logger;

import java.util.List;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    public static void logNotificationChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Logger.d(context, TAG, "logNotificationChannels: ");
        if (notificationManager != null) {
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();
            for (NotificationChannel channel : channels) {
                Logger.d(context, TAG, "Channel" + channel.toString());
                Logger.d(context, TAG, "Channel ID: " + channel.getId() + ", Name: " + channel.getName());
            }
        }
    }

    public static void updateNotificationChannel(Context context, String channelId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
            if (existingChannel != null) {
                existingChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
                existingChannel.setSound(null, null);
                existingChannel.enableVibration(false);
                existingChannel.setShowBadge(false); // Отключение отображения значка уведомлений
                existingChannel.enableLights(false); // Отключение световых сигналов
                notificationManager.createNotificationChannel(existingChannel);
            }
        }
    }
    public static void resetNotificationChannel(Context context, String channelId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Получение существующего канала
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
            if (existingChannel != null) {
                // Изменение свойств существующего канала
                existingChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
                existingChannel.setSound(null, null);
                existingChannel.enableVibration(false);
                existingChannel.setShowBadge(false);
                existingChannel.enableLights(false);
                notificationManager.createNotificationChannel(existingChannel);
            }
        }
    }

    public static void disableNotificationChannel(Context context, String channelId) {
        logNotificationChannels(context);
        NotificationUtils.resetNotificationChannel(context, channelId);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Logger.d(context, TAG, "disableNotificationChannel: ");
            // Изменение свойств канала вместо удаления
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
            if (existingChannel != null) {
                existingChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
                notificationManager.createNotificationChannel(existingChannel);
            }
        }
        logNotificationChannels(context);
    }
    public static void saveChannelCreator(Context context, String channelId, String creator) {
        SharedPreferences prefs = context.getSharedPreferences("ChannelPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(channelId, creator);
        editor.apply();
    }

    public static String getChannelCreator(Context context, String channelId) {
        SharedPreferences prefs = context.getSharedPreferences("ChannelPrefs", Context.MODE_PRIVATE);
        return prefs.getString(channelId, "Unknown");
    }
}

