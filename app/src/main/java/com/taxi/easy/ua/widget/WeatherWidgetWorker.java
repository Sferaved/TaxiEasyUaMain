package com.taxi.easy.ua.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherWidgetWorker extends Worker {

    private static final String TAG = "WeatherWidgetWorker";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String PREF_NAME = "weather_widget_prefs";
    private static final String KEY_TEMP = "last_temp";
    private static final String KEY_DESCRIPTION = "last_description";
    private static final String KEY_ICON = "last_icon";
    private static final String KEY_HUMIDITY = "last_humidity";
    private static final String KEY_CITY = "last_city";

    public interface WeatherApiService {
        @GET("weather")
        Call<WeatherResponse> getCurrentWeather(
                @Query("q") String cityName,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang
        );
    }

    // Публичный конструктор без аргументов НЕ НУЖЕН для Worker
    // WorkManager использует конструктор с Context и WorkerParameters
    public WeatherWidgetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // Добавьте в класс WeatherWidgetWorker.java

    @Override
    public Result doWork() {
        // ❌ НИЧЕГО НЕ ДЕЛАЕМ - автообновление отключено
        Logger.d(getApplicationContext(), TAG, "Auto update is disabled");
        return Result.success();
    }

    private WeatherResponse fetchWeatherDataSync(String city, String apiKey) throws InterruptedException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService service = retrofit.create(WeatherApiService.class);
        String lang = getLanguage();

        Call<WeatherResponse> call = service.getCurrentWeather(city, apiKey, "metric", lang);

        final WeatherResponse[] result = new WeatherResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result[0] = response.body();
                    Logger.d(getApplicationContext(), TAG, "Weather API response successful");
                } else {
                    Logger.e(getApplicationContext(), TAG, "Weather API response error: " + response.code());
                }
                latch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Logger.e(getApplicationContext(), TAG, "Weather API failure: " + t.getMessage());
                latch.countDown();
            }
        });

        if (latch.await(10, TimeUnit.SECONDS)) {
            return result[0];
        }
        return null;
    }
    FirestoreHelper firestoreHelper;
    SharedPreferences prefs;
    private String getApiKey() {

        if (MainActivity.weatherKey != null && !MainActivity.weatherKey.isEmpty()) {
            return MainActivity.weatherKey;
        } else {
            weatherKeyFromFb();
        }

        prefs = getApplicationContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return prefs.getString("weather_api_key", null);
    }
    private void weatherKeyFromFb() {
        firestoreHelper.getWeatherKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                MainActivity.weatherKey = vKey;
                prefs.edit().putString("weather_api_key", vKey).apply();
                Logger.d(getApplicationContext(), TAG, "weatherKey: " + vKey);
            }

            @Override
            public void onFailure(Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(getApplicationContext(), TAG, "Ошибка: " + e.getMessage());
            }
        });
    }

    private String getLanguage() {
        prefs = getApplicationContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String locale = prefs.getString("locale", "uk");
        // Конвертируем код языка для OpenWeatherMap
        switch (locale) {
            case "uk": return "ua";
            case "ru": return "ru";
            case "en": return "en";
            default: return "ua";
        }
    }

    private void saveWeatherToPrefs(WeatherResponse weather) {
        prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (weather.getMain() != null) {
            editor.putInt(KEY_TEMP, (int) Math.round(weather.getMain().getTemp()));
            editor.putInt(KEY_HUMIDITY, weather.getMain().getHumidity());
        }

        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            editor.putString(KEY_DESCRIPTION, weather.getWeather().get(0).getDescription());
            editor.putString(KEY_ICON, weather.getWeather().get(0).getIcon());
        }

        editor.putLong("last_update", System.currentTimeMillis());
        editor.apply();
    }

    private void saveCityToPrefs(String city) {
        prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CITY, city).apply();
    }

    private void loadSavedWeatherToWidget() {
        prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int temp = prefs.getInt(KEY_TEMP, 0);
        int humidity = prefs.getInt(KEY_HUMIDITY, 0);
        String description = prefs.getString(KEY_DESCRIPTION, "");
        String icon = prefs.getString(KEY_ICON, "");
        String city = prefs.getString(KEY_CITY, "Kyiv");

        if (temp != 0) {
            // Создаем объект WeatherResponse с сохраненными данными
            WeatherResponse weather = new WeatherResponse();
            WeatherResponse.Main main = new WeatherResponse.Main();
            WeatherResponse.Weather weatherInfo = new WeatherResponse.Weather();

            // Используем reflection или создаем специальный метод для установки значений
            // Временное решение - обновляем виджет напрямую с сохраненными данными
            updateWidgetWithSavedData(temp, humidity, description, icon, city);
        }
    }

    private void updateWidgetWithSavedData(int temp, int humidity, String description, String icon, String city) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName componentName = new ComponentName(getApplicationContext(), WeatherWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_weather);

            views.setTextViewText(R.id.tv_widget_city, city);
            views.setTextViewText(R.id.tv_widget_temp, temp + "°C");
            views.setTextViewText(R.id.tv_widget_description, capitalizeFirstLetter(description));
            views.setTextViewText(R.id.tv_widget_humidity, humidity + "%");

            int iconRes = getWeatherIcon(icon);
            views.setImageViewResource(R.id.iv_widget_weather_icon, iconRes);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String updateTime = getApplicationContext().getString(R.string.updated_at) + " " + timeFormat.format(new Date());
            views.setTextViewText(R.id.tv_widget_update_time, updateTime);

            // Добавляем PendingIntent для открытия приложения
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("open_weather", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(), appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private int getWeatherIcon(String iconCode) {
        if (iconCode == null) return R.drawable.ic_weather_default;

        switch (iconCode) {
            case "01d": case "01n": return R.drawable.ic_clear_sky;
            case "02d": case "02n": return R.drawable.ic_few_clouds;
            case "03d": case "03n": case "04d": case "04n": return R.drawable.ic_broken_clouds;
            case "09d": case "09n": return R.drawable.ic_shower_rain;
            case "10d": case "10n": return R.drawable.ic_rain;
            case "11d": case "11n": return R.drawable.ic_thunderstorm;
            case "13d": case "13n": return R.drawable.ic_snow;
            case "50d": case "50n": return R.drawable.ic_mist;
            default: return R.drawable.ic_weather_default;
        }
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private String getCityFromDatabase() {
        try {
            android.database.sqlite.SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(
                    MainActivity.DB_NAME, Context.MODE_PRIVATE, null);

            String city = "Kyiv";
            android.database.Cursor c = database.query(MainActivity.CITY_INFO, null, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int cityIndex = c.getColumnIndex("city");
                if (cityIndex != -1) {
                    String cityFromDb = c.getString(cityIndex);
                    if (cityFromDb != null && !cityFromDb.isEmpty()) {
                        city = convertCityName(cityFromDb);
                    }
                }
                c.close();
            }
            database.close();
            return city;
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Error reading city: " + e.getMessage());
            return "Kyiv";
        }
    }

    private String convertCityName(String cityMenu) {
        Context context = getApplicationContext();

        switch (cityMenu) {
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
//        switch (dbCity) {
//            case "Kyiv City": return "Kyiv";
//            case "Dnipropetrovsk Oblast": return "Dnipro";
//            case "Odessa": return "Odesa";
//            case "OdessaTest": return "Odesa";
//            case "Zaporizhzhia": return "Zaporizhzhia";
//            case "Cherkasy Oblast": return "Cherkasy";
//            case "Lviv": return "Lviv";
//            case "Ivano_frankivsk": return "Ivano-Frankivsk";
//            case "Vinnytsia": return "Vinnytsia";
//            case "Poltava": return "Poltava";
//            case "Sumy": return "Sumy";
//            case "Kharkiv": return "Kharkiv";
//            case "Chernihiv": return "Chernihiv";
//            case "Rivne": return "Rivne";
//            case "Ternopil": return "Ternopil";
//            case "Khmelnytskyi": return "Khmelnytskyi";
//            case "Zakarpattya": return "Uzhhorod";
//            case "Zhytomyr": return "Zhytomyr";
//            case "Kropyvnytskyi": return "Kropyvnytskyi";
//            case "Mykolaiv": return "Mykolaiv";
//            case "Chernivtsi": return "Chernivtsi";
//            case "Lutsk": return "Lutsk";
//            default: return dbCity;
    }
}