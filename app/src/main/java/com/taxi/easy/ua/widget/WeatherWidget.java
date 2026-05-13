package com.taxi.easy.ua.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
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

            // 🔥 ИСПРАВЛЕНО: Используем безопасный метод вместо прямого доступа к MyApplication
            String systemLocale = Locale.getDefault().getLanguage();
            String configLocale = context.getResources().getConfiguration().locale.getLanguage();

            Logger.d(context, TAG, "Locale.getDefault(): " + systemLocale);
            Logger.d(context, TAG, "Configuration locale: " + configLocale);
            Logger.d(context, TAG, "Process ID: " + android.os.Process.myPid());

            String localeCode = configLocale;
            Logger.d(context, TAG, "onUpdate: запрашиваем погоду на языке - " + localeCode);

            // Затем пробуем загрузить свежие данные (асинхронно)
            loadWeatherAsync(context, appWidgetManager, appWidgetId, localeCode);
        }

        // ВКЛЮЧАЕМ АВТОМАТИЧЕСКОЕ ОБНОВЛЕНИЕ
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

    /**
     * Преобразует код локали в формат OpenWeather
     */
    private static String getOpenWeatherLang(String localeCode) {
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
            default:
                return "ua";
        }
    }
    /**
     * Асинхронная загрузка свежей погоды
     */
    private static void loadWeatherAsync(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            String localeCode
    ) {
        String lang = getOpenWeatherLang(localeCode);
        String apiKey = WeatherApiHelper.getApiKey(context);
        if (apiKey == null || apiKey.isEmpty()) {
            Logger.e(context, TAG, "API Key not available");
            return;
        }

        String city = getCityFromDatabase(context);
        if (city == null || city.isEmpty()) {
            city = "Kyiv";
        }
        String requestUrl = String.format(Locale.US,
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=%s",
                city, apiKey, lang);
        Logger.d(context, TAG, "🔍 FULL WEATHER REQUEST URL: " + requestUrl);
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



    public static void updateWidgetWithData(Context context, AppWidgetManager appWidgetManager,
                                            int appWidgetId, WeatherResponse weather) {

        Logger.d(context, TAG, "═══════════════════════════════════════════");
        Logger.d(context, TAG, "📱 НАЧАЛО ОБНОВЛЕНИЯ ВИДЖЕТА (ID: " + appWidgetId + ")");
        Logger.d(context, TAG, "═══════════════════════════════════════════");

        // Проверка входных параметров
        if (context == null) {
            Logger.e(null, TAG, "❌ ОШИБКА: context = null");
            return;
        }
        if (appWidgetManager == null) {
            Logger.e(context, TAG, "❌ ОШИБКА: appWidgetManager = null");
            return;
        }
        if (weather == null) {
            Logger.e(context, TAG, "❌ ОШИБКА: weather = null");
            return;
        }

        Logger.d(context, TAG, "1️⃣ Создаём RemoteViews...");
        RemoteViews views = createBaseRemoteViews(context);
        Logger.d(context, TAG, "✅ RemoteViews создан");

        // Устанавливаем градиентный фон в зависимости от погоды
        Logger.d(context, TAG, "2️⃣ Устанавливаем фон...");
        int backgroundRes = getBackgroundForWeather(weather);
        Logger.d(context, TAG, "   → Фон ресурс ID: " + backgroundRes);
        views.setInt(R.id.widget_container, "setBackgroundResource", backgroundRes);
        Logger.d(context, TAG, "✅ Фон установлен");

        // Город
        Logger.d(context, TAG, "3️⃣ Устанавливаем город...");
        String cityName = getCityFromDatabase(context);
        Logger.d(context, TAG, "   → Название города: '" + cityName + "'");
        views.setTextViewText(R.id.tv_widget_city, cityName);
        Logger.d(context, TAG, "✅ Город установлен");

        // Температура и влажность
        Logger.d(context, TAG, "4️⃣ Устанавливаем температуру и влажность...");
        if (weather.getMain() != null) {
            double tempRaw = weather.getMain().getTemp();
            int temp = (int) Math.round(tempRaw);
            Logger.d(context, TAG, "   → Температура (raw): " + tempRaw + "°C");
            Logger.d(context, TAG, "   → Температура (округл): " + temp + "°C");
            views.setTextViewText(R.id.tv_widget_temp, temp + "°C");

            int humidityValue = weather.getMain().getHumidity();
            String humidity = humidityValue + "%";
            Logger.d(context, TAG, "   → Влажность: " + humidity);
            views.setTextViewText(R.id.tv_widget_humidity, humidity);
            Logger.d(context, TAG, "✅ Температура и влажность установлены");
        } else {
            Logger.w(context, TAG, "⚠️ weather.getMain() = null, пропускаем температуру/влажность");
        }

        // Описание и иконка
        Logger.d(context, TAG, "5️⃣ Устанавливаем описание и иконку...");
        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            String description = weather.getWeather().get(0).getDescription();
            String iconCode = weather.getWeather().get(0).getIcon();
            Logger.d(context, TAG, "   → Описание (raw): '" + description + "'");
            Logger.d(context, TAG, "   → Icon code: '" + iconCode + "'");

            String capitalizedDesc = capitalizeFirstLetter(description);
            Logger.d(context, TAG, "   → Описание (капитализ.): '" + capitalizedDesc + "'");
            views.setTextViewText(R.id.tv_widget_description, capitalizedDesc);

            int iconRes = getWeatherIcon(iconCode);
            Logger.d(context, TAG, "   → Icon resource ID: " + iconRes);
            views.setImageViewResource(R.id.iv_widget_weather_icon, iconRes);
            Logger.d(context, TAG, "✅ Описание и иконка установлены");
        } else {
            Logger.w(context, TAG, "⚠️ weather.getWeather() = null или пуст, пропускаем описание/иконку");
        }

        // Время обновления
        Logger.d(context, TAG, "6️⃣ Устанавливаем время обновления...");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();
        String timeStr = timeFormat.format(now);
        Logger.d(context, TAG, "   → Текущее время: " + timeStr);

        String updateTime = context.getString(R.string.updated_at) + " " + timeStr;
        Logger.d(context, TAG, "   → Текст времени: '" + updateTime + "'");
        views.setTextViewText(R.id.tv_widget_update_time, updateTime);
        Logger.d(context, TAG, "✅ Время обновления установлено");

        // Устанавливаем обработчик нажатия
        Logger.d(context, TAG, "7️⃣ Устанавливаем PendingIntent...");
        setPendingIntent(context, views, appWidgetId);
        Logger.d(context, TAG, "✅ PendingIntent установлен");

        // Обновляем виджет
        Logger.d(context, TAG, "8️⃣ Отправляем обновление в AppWidgetManager...");
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Logger.d(context, TAG, "✅ Виджет (ID: " + appWidgetId + ") успешно обновлён!");
        } catch (Exception e) {
            Logger.e(context, TAG, "❌ ОШИБКА при обновлении виджета: " + e.getMessage());
            e.printStackTrace();
        }

        Logger.d(context, TAG, "═══════════════════════════════════════════");
        Logger.d(context, TAG, "🏁 ЗАВЕРШЕНИЕ ОБНОВЛЕНИЯ ВИДЖЕТА (ID: " + appWidgetId + ")");
        Logger.d(context, TAG, "═══════════════════════════════════════════");
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
        Context context = MyApplication.getContext();
        Logger.d(context, TAG, "🎨 getBackgroundForWeather() вызван");

        if (weather == null) {
            Logger.w(context, TAG, "   → weather = null, используем дефолтный фон");
            return R.drawable.bg_weather_default;
        }

        if (weather.getWeather() == null || weather.getWeather().isEmpty()) {
            Logger.w(context, TAG, "   → weather.getWeather() = null/пуст, используем дефолтный фон");
            return R.drawable.bg_weather_default;
        }

        String iconCode = weather.getWeather().get(0).getIcon();
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean isDay = currentHour >= 6 && currentHour < 18;

        Logger.d(context, TAG, "   → Icon code: '" + iconCode + "'");
        Logger.d(context, TAG, "   → Текущий час: " + currentHour);
        Logger.d(context, TAG, "   → День/ночь: " + (isDay ? "ДЕНЬ" : "НОЧЬ"));

        int result;
        switch (iconCode) {
            case "01d":
                result = isDay ? R.drawable.bg_weather_clear_day : R.drawable.bg_weather_clear_night;
                Logger.d(context, TAG, "   → ЯСНО, фон: " + (isDay ? "дневной" : "ночной"));
                break;
            case "01n":
                result = R.drawable.bg_weather_clear_night;
                Logger.d(context, TAG, "   → ЯСНО (ночь), фон: ночной");
                break;
            case "02d":
            case "03d":
            case "04d":
                result = isDay ? R.drawable.bg_weather_clouds_day : R.drawable.bg_weather_clouds_night;
                Logger.d(context, TAG, "   → ОБЛАЧНО, фон: " + (isDay ? "дневной" : "ночной"));
                break;
            case "02n":
            case "03n":
            case "04n":
                result = R.drawable.bg_weather_clouds_night;
                Logger.d(context, TAG, "   → ОБЛАЧНО (ночь), фон: ночной");
                break;
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                result = R.drawable.bg_weather_rain;
                Logger.d(context, TAG, "   → ДОЖДЬ, фон: дождливый");
                break;
            case "11d":
            case "11n":
                result = R.drawable.bg_weather_thunderstorm;
                Logger.d(context, TAG, "   → ГРОЗА, фон: грозовой");
                break;
            case "13d":
            case "13n":
                result = R.drawable.bg_weather_snow;
                Logger.d(context, TAG, "   → СНЕГ, фон: снежный");
                break;
            case "50d":
            case "50n":
                result = R.drawable.bg_weather_fog;
                Logger.d(context, TAG, "   → ТУМАН, фон: туманный");
                break;
            default:
                result = R.drawable.bg_weather_default;
                Logger.d(context, TAG, "   → НЕИЗВЕСТНЫЙ КОД '" + iconCode + "', используем дефолтный фон");
                break;
        }

        Logger.d(context, TAG, "   → Выбран фон: " + result);
        return result;
    }

    private static void setPendingIntent(Context context, RemoteViews views, int appWidgetId) {
        // Существующий Intent для открытия приложения
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra("open_weather", true);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent);

        // ДОБАВЬТЕ ЭТОТ БЛОК для кнопки уведомления
        Intent notificationIntent = new Intent(context, WeatherNotificationReceiver.class);
        notificationIntent.setAction("SEND_WEATHER_NOTIFICATION");
        PendingIntent notificationPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 1000, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btn_weather_notification, notificationPendingIntent);
    }

    static void scheduleWork(Context context) {
        try {
            // Минимальный интервал - 1 час (рекомендуется для экономии батареи)
            // Можно установить 15 минут, но система может округлить до 1 часа
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    WeatherWidgetWorker.class,
                    15, TimeUnit.HOURS,  // Интервал обновления
                    15, TimeUnit.MINUTES // Допустимое отклонение
            ).build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Не заменять если уже есть
                    workRequest
            );

            Logger.d(context, TAG, "✅ Периодическое обновление запланировано (каждый час)");
        } catch (Exception e) {
            Logger.e(context, TAG, "❌ Ошибка при планировании обновления: " + e.getMessage());
        }
    }

    // ДОБАВЬТЕ метод для принудительного обновления (для кнопки)
    public static void forceUpdate(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, WeatherWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                // Показываем кэш
                for (int appWidgetId : appWidgetIds) {
                    WeatherResponse cachedWeather = WeatherApiHelper.getCachedWeather(context);
                    if (cachedWeather != null) {
                        updateWidgetWithData(context, appWidgetManager, appWidgetId, cachedWeather);
                    }
                }

                // Запускаем Worker для фонового обновления
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherWidgetWorker.class)
                        .build();
                WorkManager.getInstance(context).enqueue(workRequest);

                Logger.d(context, TAG, "🔄 Принудительное обновление запущено");
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка при принудительном обновлении: " + e.getMessage());
        }
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

    static String getCityFromDatabase(Context context) {
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

    // Добавьте этот метод в класс WeatherWidget.java

    /**
     * Проверяет погоду и отправляет уведомление, если погода изменилась или прошло достаточно времени
     */
    public static void checkAndSendWeatherNotification(Context context) {
        Logger.d(context, TAG, "🔔 checkAndSendWeatherNotification - START");

        WeatherResponse cachedWeather = WeatherApiHelper.getCachedWeather(context);

        if (cachedWeather == null || cachedWeather.getMain() == null) {
            Logger.d(context, TAG, "❌ Нет кэшированной погоды для уведомления");
            return;
        }

        Logger.d(context, TAG, "✅ Кэш есть, температура: " + cachedWeather.getMain().getTemp());

        long lastNotificationTime = getLastNotificationTime(context);
        long currentTime = System.currentTimeMillis();
        long timeSinceLastNotification = currentTime - lastNotificationTime;

        Logger.d(context, TAG, "⏰ Время с последнего уведомления: " + (timeSinceLastNotification / 1000 / 60) + " минут");

        // Временно отключим проверку для теста
        // if (timeSinceLastNotification < 3 * 60 * 60 * 1000) {
        //     Logger.d(context, TAG, "Уведомление отправлялось недавно, пропускаем");
        //     return;
        // }

        String cityName = getCityFromDatabase(context);
        Logger.d(context, TAG, "🏙️ Город: " + cityName);

        Logger.d(context, TAG, "📤 Отправляем уведомление...");
        WeatherNotificationHelper.showWeatherNotification(context, cachedWeather, cityName);

        saveLastNotificationTime(context, currentTime);
        Logger.d(context, TAG, "✅ checkAndSendWeatherNotification - END");
    }

    private static long getLastNotificationTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE);
        return prefs.getLong("last_notification_time", 0);
    }

    private static void saveLastNotificationTime(Context context, long time) {
        SharedPreferences prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_notification_time", time).apply();
    }

}