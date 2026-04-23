package com.taxi.easy.ua.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.weather.WeatherApiHelper;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.log.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WeatherWidget extends AppWidgetProvider {

    private static final String TAG = "WeatherWidget";
    private static final String WORK_NAME = "WeatherWidgetUpdateWork";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // Сначала показываем кэшированные данные (быстро)
            showCachedWeather(context, appWidgetManager, appWidgetId);

            // Затем пробуем загрузить свежие данные (асинхронно)
            loadWeatherAsync(context, appWidgetManager, appWidgetId);
        }
        scheduleWork(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleWork(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    /**
     * Быстрый показ кэшированной погоды (первый показ без зависания)
     */
    private void showCachedWeather(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        WeatherResponse cachedWeather = WeatherApiHelper.getCachedWeather(context);

        if (cachedWeather != null && cachedWeather.getMain() != null) {
            Logger.d(context, TAG, "Showing cached weather");
            updateWidgetWithData(context, appWidgetManager, appWidgetId, cachedWeather);
        } else {
            // Если кэша нет, показываем заглушку
            RemoteViews views = createBaseRemoteViews(context);
            views.setTextViewText(R.id.tv_widget_temp, "--°C");
            views.setTextViewText(R.id.tv_widget_city, context.getString(R.string.loading));
            views.setTextViewText(R.id.tv_widget_description, context.getString(R.string.wait_update));
            views.setImageViewResource(R.id.iv_widget_weather_icon, R.drawable.ic_weather_default);
            views.setTextViewText(R.id.tv_widget_humidity, "--%");

            // Устанавливаем дефолтный фон
            views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.bg_weather_default);

            // Устанавливаем обработчик нажатия
            setPendingIntent(context, views, appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Асинхронная загрузка свежей погоды
     */
    private void loadWeatherAsync(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String apiKey = WeatherApiHelper.getApiKey(context);
        if (apiKey == null || apiKey.isEmpty()) {
            Logger.e(context, TAG, "API Key not available");
            return;
        }

        String city = getCityFromDatabase(context);
        if (city == null || city.isEmpty()) {
            city = "Kyiv";
        }

        WeatherApiHelper.fetchWeatherAsync(context, city, apiKey, new WeatherApiHelper.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse weather) {
                Logger.d(context, TAG, "Fresh weather loaded");
                updateWidgetWithData(context, appWidgetManager, appWidgetId, weather);
            }

            @Override
            public void onFailure(String error) {
                Logger.e(context, TAG, "Failed to load fresh weather: " + error);
                // Если есть кэш, он уже показан, ничего не делаем
            }
        });
    }

    public static void updateAllWidgets(Context context, WeatherResponse weather) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WeatherWidget.class));

        for (int appWidgetId : appWidgetIds) {
            updateWidgetWithData(context, appWidgetManager, appWidgetId, weather);
        }
    }

    private static void updateWidgetWithData(Context context, AppWidgetManager appWidgetManager,
                                             int appWidgetId, WeatherResponse weather) {
        RemoteViews views = createBaseRemoteViews(context);

        // Устанавливаем градиентный фон в зависимости от погоды
        int backgroundRes = getBackgroundForWeather(weather);
        views.setInt(R.id.widget_container, "setBackgroundResource", backgroundRes);

        // Город
        String cityName = getCityFromDatabase(context);
        views.setTextViewText(R.id.tv_widget_city, cityName);

        // Температура
        if (weather.getMain() != null) {
            int temp = (int) Math.round(weather.getMain().getTemp());
            views.setTextViewText(R.id.tv_widget_temp, temp + "°C");

            // Влажность
            String humidity = weather.getMain().getHumidity() + "%";
            views.setTextViewText(R.id.tv_widget_humidity, humidity);
        }

        // Описание и иконка
        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            String description = weather.getWeather().get(0).getDescription();
            String capitalizedDesc = capitalizeFirstLetter(description);
            views.setTextViewText(R.id.tv_widget_description, capitalizedDesc);

            String iconCode = weather.getWeather().get(0).getIcon();
            int iconRes = getWeatherIcon(iconCode);
            views.setImageViewResource(R.id.iv_widget_weather_icon, iconRes);
        }

        // Время обновления
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String updateTime = context.getString(R.string.updated_at) + " " + timeFormat.format(new Date());
        views.setTextViewText(R.id.tv_widget_update_time, updateTime);

        // Устанавливаем обработчик нажатия
        setPendingIntent(context, views, appWidgetId);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Создаёт базовый RemoteViews с правильным layout
     */
    private static RemoteViews createBaseRemoteViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_weather);
    }

    /**
     * Возвращает ресурс градиента в зависимости от погоды
     */
    private static int getBackgroundForWeather(WeatherResponse weather) {
        if (weather == null || weather.getWeather() == null || weather.getWeather().isEmpty()) {
            return R.drawable.bg_weather_default;
        }

        String iconCode = weather.getWeather().get(0).getIcon();

        // Определяем, день сейчас или ночь (для реалистичности)
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean isDay = currentHour >= 6 && currentHour < 18;

        switch (iconCode) {
            // Ясно
            case "01d":
                return isDay ? R.drawable.bg_weather_clear_day : R.drawable.bg_weather_clear_night;
            case "01n":
                return R.drawable.bg_weather_clear_night;

            // Малооблачно / Облачно
            case "02d":
            case "03d":
            case "04d":
                return isDay ? R.drawable.bg_weather_clouds_day : R.drawable.bg_weather_clouds_night;
            case "02n":
            case "03n":
            case "04n":
                return R.drawable.bg_weather_clouds_night;

            // Дождь
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                return R.drawable.bg_weather_rain;

            // Гроза
            case "11d":
            case "11n":
                return R.drawable.bg_weather_thunderstorm;

            // Снег
            case "13d":
            case "13n":
                return R.drawable.bg_weather_snow;

            // Туман
            case "50d":
            case "50n":
                return R.drawable.bg_weather_fog;

            default:
                return R.drawable.bg_weather_default;
        }
    }

    private static void setPendingIntent(Context context, RemoteViews views, int appWidgetId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_weather", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
    }

    private static void scheduleWork(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WeatherWidgetWorker.class,
                1, TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }

    private static int getWeatherIcon(String iconCode) {
        switch (iconCode) {
            case "01d":
            case "01n":
                return R.drawable.ic_clear_sky;
            case "02d":
            case "02n":
                return R.drawable.ic_few_clouds;
            case "03d":
            case "03n":
            case "04d":
            case "04n":
                return R.drawable.ic_broken_clouds;
            case "09d":
            case "09n":
                return R.drawable.ic_shower_rain;
            case "10d":
            case "10n":
                return R.drawable.ic_rain;
            case "11d":
            case "11n":
                return R.drawable.ic_thunderstorm;
            case "13d":
            case "13n":
                return R.drawable.ic_snow;
            case "50d":
            case "50n":
                return R.drawable.ic_mist;
            default:
                return R.drawable.ic_weather_default;
        }
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private static String getCityFromDatabase(Context context) {
        // Получаем город из вашей БД как в HistoryFragment
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        if (stringList == null || stringList.size() < 2) {
            return context.getString(R.string.Kyiv_city);
        }

        String city = stringList.get(1);
        String cityMenu;
        switch (city) {
            case "Kyiv City":
                cityMenu = context.getString(R.string.Kyiv_city);
                break;
            case "Dnipropetrovsk Oblast":
                cityMenu = context.getString(R.string.Dnipro_city);
                break;
            case "Odessa":
            case "OdessaTest":
                cityMenu = context.getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                cityMenu = context.getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                cityMenu = context.getString(R.string.Cherkasy);
                break;
            case "Lviv":
                cityMenu = context.getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                cityMenu = context.getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                cityMenu = context.getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                cityMenu = context.getString(R.string.city_poltava);
                break;
            case "Sumy":
                cityMenu = context.getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                cityMenu = context.getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                cityMenu = context.getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                cityMenu = context.getString(R.string.city_rivne);
                break;
            case "Ternopil":
                cityMenu = context.getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                cityMenu = context.getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                cityMenu = context.getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                cityMenu = context.getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                cityMenu = context.getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                cityMenu = context.getString(R.string.city_mykolaiv);
                break;
            case "Chernivtsi":
                cityMenu = context.getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                cityMenu = context.getString(R.string.city_lutsk);
                break;
            default:
                cityMenu = context.getString(R.string.Kyiv_city);
        }
        return cityMenu;
    }

    @SuppressLint("Range")
    private static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase database = null;

        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
            try (android.database.Cursor c = database.query(table, null, null, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    do {
                        for (String cn : c.getColumnNames()) {
                            String value = c.getString(c.getColumnIndex(cn));
                            if (value != null) {
                                list.add(value);
                            }
                        }
                    } while (c.moveToNext());
                }
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error reading from database: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
        return list;
    }
}