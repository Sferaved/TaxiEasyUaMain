package com.taxi.easy.ua.ui.weather.finish;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CityInfoHelper {
    private static final String BASE_URL = "https://City-Info.utax.top/api/data/";
    private static final String TOKEN = "ari_iMYCEh8wbb4hQSVqlFDzN8G-9RAhPTbSxe7eO9rAGOA";

    private OkHttpClient client;

    public CityInfoHelper() {
        client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    public interface CityInfoCallback {
        void onSuccess(CityInfo info);
        void onError(String error);
    }

    public void getCityInfo(String city, CityInfoCallback callback) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url(BASE_URL + city)
                    .addHeader("Authorization", "Bearer " + TOKEN)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    CityInfo info = parseJson(json);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(info));
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("HTTP " + response.code()));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e.getMessage()));
            }
        }).start();
    }

    private CityInfo parseJson(String json) throws Exception {
        JSONObject obj = new JSONObject(json);
        CityInfo info = new CityInfo();
        info.setWeather(obj.optString("weather", ""));
        info.setTemperature(obj.optDouble("temperature", 0));
        info.setAirAlarm(obj.optBoolean("air_alarm", false));
        info.setRebActive(obj.optBoolean("reb_active", false));
        info.setTimeStamp(obj.optString("time_stamp", ""));
        return info;
    }
}
