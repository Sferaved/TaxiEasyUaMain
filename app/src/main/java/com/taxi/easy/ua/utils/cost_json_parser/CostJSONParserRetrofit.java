package com.taxi.easy.ua.utils.cost_json_parser;

import static com.taxi.easy.ua.MainActivity.costMap;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.util.Log;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CostJSONParserRetrofit {

    private static final String TAG = "CostJSONParser";
    private final APIService apiService;

    public CostJSONParserRetrofit() {
//        Retrofit retrofit = RetrofitClient.getClient("https://m.easy-order-taxi.site");
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Создание клиента OkHttpClient с подключенным логгером
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(loggingInterceptor);
        httpClient.connectTimeout(60, TimeUnit.SECONDS); // Тайм-аут для соединения
        httpClient.readTimeout(60, TimeUnit.SECONDS);    // Тайм-аут для чтения
        httpClient.writeTimeout(60, TimeUnit.SECONDS);   // Тайм-аут для записи
        // httpClient.addInterceptor(loggingInterceptor);
        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build()) // Подключение клиента OkHttpClient с логгером
                .build();


        apiService = retrofit.create(APIService.class);
    }
    private volatile boolean eventReceived = false;
    private Call<Map<String, String>> activeCall;

    public void sendURL(String urlString, final Callback<Map<String, String>> callback) throws MalformedURLException {
        Call<Map<String, String>> call = apiService.getData(urlString);
        activeCall = call;

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (eventReceived) {
                    Log.d("API_CALL", "HTTP-ответ отменен, так как событие уже получено.");
                    callback.onResponse(call, Response.success(costMap));
                    return;
                }
                Map<String, String> costMap = new HashMap<>();
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> jsonResponse = response.body();
                    if (!"0".equals(jsonResponse.get("order_cost"))) {
                        costMap.putAll(jsonResponse);
                    } else {
                        costMap.put("order_cost", "0");
                        costMap.put("Message", jsonResponse.get("Message"));
                    }
                } else {
                    costMap.put("order_cost", "0");
                    costMap.put("Message", "ErrorMessage");
                }
                callback.onResponse(call, Response.success(costMap));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Map<String, String> costMap = new HashMap<>();
                costMap.put("order_cost", "0");
                costMap.put("Message", "ErrorMessage");
                callback.onResponse(call, Response.success(costMap));
            }
        });

//        // Ожидаем событие
        new Thread(() -> {
            while (!eventReceived) {
                if (costMap != null && !costMap.isEmpty()) {
                    eventReceived = true;
                    if (activeCall != null && !activeCall.isExecuted()) {
                        activeCall.cancel(); // Прерываем запрос
                        Log.d("API_CALL", "HTTP-запрос прерван из-за события.");
                    }
                    callback.onResponse(call, Response.success(costMap));
                }
                try {
                    Thread.sleep(100); // Ожидание события с минимальной задержкой
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();
    }
}
