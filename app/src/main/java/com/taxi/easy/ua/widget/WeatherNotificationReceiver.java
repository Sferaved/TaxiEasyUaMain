package com.taxi.easy.ua.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.taxi.easy.ua.ui.weather.WeatherApiHelper;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.log.Logger;

public class WeatherNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "WeatherNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "SEND_WEATHER_NOTIFICATION".equals(intent.getAction())) {
            Logger.d(context, TAG, "Ручной запрос на отправку уведомления о погоде");

            // Получаем кэшированную погоду и отправляем уведомление
            WeatherResponse weather = WeatherApiHelper.getCachedWeather(context);
            if (weather != null && weather.getMain() != null) {
                String cityName = WeatherWidget.getCityFromDatabase(context);
                WeatherNotificationHelper.showWeatherNotification(context, weather, cityName);
                Logger.d(context, TAG, "Уведомление отправлено вручную");
            } else {
                Logger.d(context, TAG, "Нет кэша погоды для отправки");
            }
        }
    }
}