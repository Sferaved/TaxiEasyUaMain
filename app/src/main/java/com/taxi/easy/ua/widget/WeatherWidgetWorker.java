package com.taxi.easy.ua.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.ui.weather.WeatherApiHelper;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.log.Logger;

public class WeatherWidgetWorker extends Worker {

    private static final String TAG = "WeatherWidgetWorker";

    public WeatherWidgetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Logger.d(context, TAG, "🔄 Автообновление погоды запущено");

        try {
            // Получаем язык из настроек
            String localeCode = context.getResources().getConfiguration().locale.getLanguage();
            String lang = getOpenWeatherLang(localeCode);

            // Получаем API ключ
            String apiKey = WeatherApiHelper.getApiKey(context);
            if (apiKey == null || apiKey.isEmpty()) {
                Logger.e(context, TAG, "❌ API Key отсутствует");
                return Result.failure();
            }

            // Получаем город
            String city = WeatherWidget.getCityFromDatabase(context);
            if (city == null || city.isEmpty()) {
                city = "Kyiv";
            }

            Logger.d(context, TAG, "📡 Запрашиваем погоду для города: " + city + ", язык: " + lang);

            // Загружаем погоду синхронно
            WeatherResponse weather = WeatherApiHelper.fetchWeatherSync(context, city, apiKey);

            if (weather != null && weather.getMain() != null) {
                // Сохраняем в кэш
                WeatherApiHelper.saveWeatherToCache(context, weather, city);

                // Обновляем все виджеты
                updateAllWidgets(context, weather);

                // Проверяем и отправляем уведомление (если нужно)
                checkAndSendNotificationIfNeeded(context, weather);

                Logger.d(context, TAG, "✅ Погода успешно обновлена: " + weather.getMain().getTemp() + "°C, " +
                        weather.getWeather().get(0).getDescription());
                return Result.success();
            } else {
                Logger.e(context, TAG, "❌ Не удалось получить данные о погоде");
                return Result.retry();
            }

        } catch (Exception e) {
            Logger.e(context, TAG, "❌ Ошибка при обновлении погоды: " + e.getMessage());
            e.printStackTrace();
            return Result.retry();
        }
    }

    /**
     * Преобразует код локали в формат OpenWeather
     */
    private String getOpenWeatherLang(String localeCode) {
        if (localeCode == null) {
            return "ua";
        }

        switch (localeCode.toLowerCase()) {
            case "ru":
            case "russian":
                return "ru";
            case "en":
            case "english":
                return "en";
            case "ua":
            case "uk":
            case "ukrainian":
                return "ua";
            default:
                return "ua";
        }
    }

    /**
     * Обновляет все виджеты погоды
     */
    private void updateAllWidgets(Context context, WeatherResponse weather) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, WeatherWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                Logger.d(context, TAG, "📱 Обновляем " + appWidgetIds.length + " виджет(ов)");

                for (int appWidgetId : appWidgetIds) {
                    // Используем публичный метод из WeatherWidget
                    WeatherWidget.updateWidgetWithData(context, appWidgetManager, appWidgetId, weather);
                }
            } else {
                Logger.d(context, TAG, "⚠️ Нет активных виджетов для обновления");
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка при обновлении виджетов: " + e.getMessage());
        }
    }

    /**
     * Проверяет и отправляет уведомление если нужно
     */
    private void checkAndSendNotificationIfNeeded(Context context, WeatherResponse weather) {
        try {
            long lastNotificationTime = getLastNotificationTime(context);
            long currentTime = System.currentTimeMillis();
            long timeSinceLastNotification = currentTime - lastNotificationTime;

            // Отправляем уведомление не чаще чем раз в 3 часа
            if (timeSinceLastNotification >= 3 * 60 * 60 * 1000) {
                String cityName = WeatherWidget.getCityFromDatabase(context);
                WeatherNotificationHelper.showWeatherNotification(context, weather, cityName);
                saveLastNotificationTime(context, currentTime);
                Logger.d(context, TAG, "🔔 Уведомление отправлено");
            } else {
                long minutesLeft = (3 * 60 * 60 * 1000 - timeSinceLastNotification) / 1000 / 60;
                Logger.d(context, TAG, "⏰ Уведомление не отправлено (следующее через " + minutesLeft + " мин)");
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка при отправке уведомления: " + e.getMessage());
        }
    }

    private long getLastNotificationTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE);
        return prefs.getLong("last_notification_time", 0);
    }

    private void saveLastNotificationTime(Context context, long time) {
        SharedPreferences prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_notification_time", time).apply();
    }
}