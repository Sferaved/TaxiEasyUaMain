package com.taxi.easy.ua.ui.weather;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
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

public class WeatherApiHelper {

    private static final String TAG = "WeatherApiHelper";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String PREF_NAME = "app_prefs";
    private static final String KEY_WEATHER_API = "weather_api_key";
    private static final String KEY_LAST_TEMP = "last_temp";
    private static final String KEY_LAST_HUMIDITY = "last_humidity";
    private static final String KEY_LAST_DESCRIPTION = "last_description";
    private static final String KEY_LAST_ICON = "last_icon";
    private static final String KEY_LAST_CITY = "last_city";
    private static final String KEY_LAST_UPDATE = "last_update";

    public interface WeatherApiService {
        @GET("weather")
        Call<WeatherResponse> getCurrentWeather(
                @Query("q") String cityName,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang
        );
    }

    public static String getApiKey(Context context) {
        // Сначала проверяем MainActivity
        if (com.taxi.easy.ua.MainActivity.weatherKey != null &&
                !com.taxi.easy.ua.MainActivity.weatherKey.isEmpty()) {
            return com.taxi.easy.ua.MainActivity.weatherKey;
        }

        // Затем SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WEATHER_API, null);
    }

    public static void saveApiKey(Context context, String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WEATHER_API, apiKey).apply();
    }

    /**
     * Синхронная загрузка погоды для первого показа виджета
     * Не блокирует UI, но дожидается результата
     */
    public static WeatherResponse fetchWeatherSync(Context context, String city, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            Logger.e(context, TAG, "API Key is null or empty");
            return null;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService service = retrofit.create(WeatherApiService.class);
        String lang = getLanguage(context);

        Call<WeatherResponse> call = service.getCurrentWeather(city, apiKey, "metric", lang);

        final WeatherResponse[] result = new WeatherResponse[1];
        final CountDownLatch latch = new CountDownLatch(1);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getWeather() != null && !response.body().getWeather().isEmpty()) {
                        String desc = response.body().getWeather().get(0).getDescription();
                        Logger.d(context, TAG, "📡 ПОЛУЧЕНО ОТ API: description = '" + desc + "'");
                    }
                    result[0] = response.body();
                    // Сохраняем данные для кэша
                    saveWeatherToCache(context, response.body(), city);
                }
                latch.countDown();
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Logger.e(context, TAG, "Fetch failed: " + t.getMessage());
                latch.countDown();
            }
        });

        try {
            // Ждем до 5 секунд
            if (latch.await(5, TimeUnit.SECONDS)) {
                return result[0];
            }
        } catch (InterruptedException e) {
            Logger.e(context, TAG, "Interrupted: " + e.getMessage());
        }
        return null;
    }

    /**
     * Асинхронная загрузка погоды (для обычного использования)
     */
    public static void fetchWeatherAsync(Context context, String city, String apiKey, WeatherCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            Logger.e(context, TAG, "API Key is null or empty");
            if (callback != null) callback.onFailure("API Key is null");
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService service = retrofit.create(WeatherApiService.class);
        String lang = getLanguage(context);

        Call<WeatherResponse> call = service.getCurrentWeather(city, apiKey, "metric", lang);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveWeatherToCache(context, response.body(), city);
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onFailure("Response error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Logger.e(context, TAG, "Fetch failed: " + t.getMessage());
                if (callback != null) callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Получение кэшированной погоды (быстрое отображение)
     */
    public static WeatherResponse getCachedWeather(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int temp = prefs.getInt(KEY_LAST_TEMP, 0);
        if (temp == 0) return null;

        int humidity = prefs.getInt(KEY_LAST_HUMIDITY, 0);
        String description = prefs.getString(KEY_LAST_DESCRIPTION, "");
        String icon = prefs.getString(KEY_LAST_ICON, "");
        String city = prefs.getString(KEY_LAST_CITY, "Kyiv");
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);

        WeatherResponse weather = new WeatherResponse();
        WeatherResponse.Main main = new WeatherResponse.Main();
        WeatherResponse.Weather weatherInfo = new WeatherResponse.Weather();

        main.setTemp(temp);
        main.setHumidity(humidity);
        weather.setMain(main);

        weatherInfo.setDescription(description);
        weatherInfo.setIcon(icon);
        weather.setWeather(java.util.Collections.singletonList(weatherInfo));
        weather.setName(city);

        return weather;
    }
    /**
     * Публичный метод для кэширования погоды
     * Использует существующий приватный метод saveWeatherToCache
     */
    public static void cacheWeather(Context context, WeatherResponse weather) {
        if (context == null || weather == null) {
            Logger.e(context, TAG, "Cannot cache weather: context or weather is null");
            return;
        }

        // Получаем город из weather или используем последний сохраненный
        String city = weather.getName();
        if (city == null || city.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            city = prefs.getString(KEY_LAST_CITY, "Kyiv");
        }

        // Вызываем существующий приватный метод
        saveWeatherToCache(context, weather, city);
    }

    /**
     * Сохранение погоды в кэш
     */
    public static void saveWeatherToCache(Context context, WeatherResponse weather, String city) {
        if (weather != null && weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            String descBefore = weather.getWeather().get(0).getDescription();
            Logger.d(context, TAG, "💾 СОХРАНЯЕМ В КЭШ: description = '" + descBefore + "'");
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (weather.getMain() != null) {
            editor.putInt(KEY_LAST_TEMP, (int) Math.round(weather.getMain().getTemp()));
            editor.putInt(KEY_LAST_HUMIDITY, weather.getMain().getHumidity());
        }

        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            editor.putString(KEY_LAST_DESCRIPTION, weather.getWeather().get(0).getDescription());
            editor.putString(KEY_LAST_ICON, weather.getWeather().get(0).getIcon());
        }

        editor.putString(KEY_LAST_CITY, city);
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
        editor.apply();

        Logger.d(context, TAG, "Weather cached successfully");
    }

    private static String getLanguage(Context context) {

        String localeCode = context.getResources().getConfiguration().locale.getLanguage();
        Logger.d(context, TAG, "fetchWeatherOnly: запрашиваем погоду на языке - " + localeCode);
        switch (localeCode) {
            case "uk": return "ua";
            case "ru": return "ru";
            case "en": return "en";
            default: return "ua";
        }
    }

    public interface WeatherCallback {
        void onSuccess(WeatherResponse weather);
        void onFailure(String error);
    }

    /**
     * Получает погоду асинхронно с указанной локалью
     * @param context Контекст
     * @param city Название города
     * @param apiKey API ключ OpenWeather
     * @param localeCode Код локали (ua, ru, en и т.д.)
     * @param callback Колбэк для результата
     */
    public static void fetchWeatherAsyncWithLocale(Context context, String city, String apiKey,
                                                   String localeCode, WeatherCallback callback) {
        // Преобразуем код локали в формат OpenWeather
        String lang = getOpenWeatherLang(localeCode);

        // Формируем URL с параметром lang
        String url = String.format(Locale.US,
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=%s",
                city, apiKey, lang);

        Logger.d(context, TAG, "fetchWeatherAsyncWithLocale: запрос к OpenWeather с локалью " + lang);
        Logger.d(context, TAG, "fetchWeatherAsyncWithLocale: URL=" + url);

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherResponse weatherResponse = parseWeatherResponse(response);
                        Logger.d(context, TAG, "fetchWeatherAsyncWithLocale: успешно, погода=" +
                                (weatherResponse.getWeather() != null && !weatherResponse.getWeather().isEmpty() ?
                                        weatherResponse.getWeather().get(0).getDescription() : "null"));
                        if (callback != null) {
                            callback.onSuccess(weatherResponse);
                        }
                    } catch (Exception e) {
                        Logger.e(context, TAG, "fetchWeatherAsyncWithLocale: ошибка парсинга - " + e.getMessage());
                        if (callback != null) {
                            callback.onFailure("Ошибка парсинга: " + e.getMessage());
                        }
                    }
                },
                error -> {
                    String errorMsg = "Ошибка запроса: ";
                    if (error.networkResponse != null) {
                        errorMsg += "код " + error.networkResponse.statusCode;
                    } else {
                        errorMsg += error.getMessage();
                    }
                    Logger.e(context, TAG, "fetchWeatherAsyncWithLocale: " + errorMsg);
                    if (callback != null) {
                        callback.onFailure(errorMsg);
                    }
                });

        queue.add(request);
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
     * Парсит JSON ответ от OpenWeather в объект WeatherResponse
     */
    private static WeatherResponse parseWeatherResponse(JSONObject response) throws JSONException {
        WeatherResponse weatherResponse = new WeatherResponse();

        // Парсим Main (температура)
        if (response.has("main")) {
            JSONObject main = response.getJSONObject("main");
            WeatherResponse.Main mainData = new WeatherResponse.Main();

            if (main.has("temp")) {
                mainData.setTemp(main.getDouble("temp"));
            }
            if (main.has("feels_like")) {
                mainData.setFeelsLike(main.getDouble("feels_like"));
            }
            if (main.has("humidity")) {
                mainData.setHumidity(main.getInt("humidity"));
            }
            if (main.has("pressure")) {
                mainData.setPressure(main.getInt("pressure"));
            }

            weatherResponse.setMain(mainData);
        }

        // Парсим Weather (описание погоды)
        if (response.has("weather")) {
            org.json.JSONArray weatherArray = response.getJSONArray("weather");
            if (weatherArray.length() > 0) {
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                WeatherResponse.Weather weather = new WeatherResponse.Weather();

                if (weatherObj.has("id")) {
                    weather.setId(weatherObj.getInt("id"));
                }
                if (weatherObj.has("main")) {
                    weather.setMain(weatherObj.getString("main"));
                }
                if (weatherObj.has("description")) {
                    weather.setDescription(weatherObj.getString("description"));
                }
                if (weatherObj.has("icon")) {
                    weather.setIcon(weatherObj.getString("icon"));
                }

                List<WeatherResponse.Weather> weatherList = new ArrayList<>();
                weatherList.add(weather);
                weatherResponse.setWeather(weatherList);
            }
        }

        // Парсим Wind (ветер)
        if (response.has("wind")) {
            JSONObject wind = response.getJSONObject("wind");
            WeatherResponse.Wind windData = new WeatherResponse.Wind();

            if (wind.has("speed")) {
                windData.setSpeed(wind.getDouble("speed"));
            }
            if (wind.has("deg")) {
                windData.setDeg(wind.getInt("deg"));
            }

            weatherResponse.setWind(windData);
        }

        // Парсим name (название города)
        if (response.has("name")) {
            weatherResponse.setName(response.getString("name"));
        }

        return weatherResponse;
    }
}