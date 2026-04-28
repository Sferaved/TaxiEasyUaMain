package com.taxi.easy.ua.widget;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.log.Logger;

public class WeatherNotificationHelper {

    private static final String CHANNEL_ID = "weather_notifications";
    private static final String CHANNEL_NAME = "Погодные уведомления";
    private static final int NOTIFICATION_ID = 1001;

    public static void showWeatherNotification(Context context, WeatherResponse weather, String cityName) {

        if (context == null || weather == null || weather.getWeather() == null || weather.getWeather().isEmpty()) {
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // ПРИНУДИТЕЛЬНОЕ УДАЛЕНИЕ СТАРОГО КАНАЛА
        manager.deleteNotificationChannel(CHANNEL_ID);

        // СОЗДАЕМ НОВЫЙ КАНАЛ С ID "default" (РАБОТАЕТ ВСЕГДА)
        String channelId = "default";
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "Уведомления",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Все уведомления приложения");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.setShowBadge(true);
        channel.setBypassDnd(true);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);

        // Формируем текст
        int temperature = (int) Math.round(weather.getMain().getTemp());
        String description = weather.getWeather().get(0).getDescription();
// Первая буква маленькая, например: "хмарно" вместо "Хмарно"
        String formattedDescription = description.substring(0, 1).toLowerCase() + description.substring(1);

        String title = String.format(context.getString(R.string.wether), cityName);
        String content = String.format(context.getString(R.string.check), formatTemperature(temperature), formattedDescription);

        // Intent
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_weather", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Уведомление с МАКСИМАЛЬНЫМИ настройками
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);


        int uniqueId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        manager.notify(uniqueId, builder.build());
        android.util.Log.d("WeatherNotification", "✅ Уведомление отправлено с ID: " + uniqueId);


    }

     private static String formatTemperature(int temperature) {
        if (temperature > 0) {
            return "+" + temperature + "°C";
        } else {
            return temperature + "°C";
        }
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    private static String lowercaseFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        // Первая буква маленькая, остальные без изменений
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }
}