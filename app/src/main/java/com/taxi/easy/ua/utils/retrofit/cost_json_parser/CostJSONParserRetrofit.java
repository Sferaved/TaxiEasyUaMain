package com.taxi.easy.ua.utils.retrofit.cost_json_parser;

import static com.taxi.easy.ua.MainActivity.costMap;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.retrofit.APIService;

import java.io.IOException;
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
        httpClient.addInterceptor(new RetryInterceptor()); // 3 попытки
        httpClient.addInterceptor(loggingInterceptor);
        httpClient.connectTimeout(10, TimeUnit.SECONDS); // Тайм-аут для соединения
        httpClient.readTimeout(10, TimeUnit.SECONDS);    // Тайм-аут для чтения
        httpClient.writeTimeout(10, TimeUnit.SECONDS);   // Тайм-аут для записи
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

        // --- ТАЙМАУТ НА 30 СЕК --- НИЧЕГО НЕ ЛОМАЕТ
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            if (activeCall != null && !activeCall.isCanceled() && !eventReceived) {
//                activeCall.cancel();
//                Log.e(TAG, "Таймаут: 15 сек — запрос отменён автоматически");
//
//                Map<String, String> timeoutMap = new HashMap<>();
//                timeoutMap.put("order_cost", "0");
//                timeoutMap.put("Message", "Таймаут: нет ответа от сервера");
//
//                callback.onResponse(activeCall, Response.success(timeoutMap));
//            }
//        }, 15_000);
        // --------------------------

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                Map<String, String> costMap = new HashMap<>();

                if (eventReceived) {
                    Log.d(TAG, "HTTP-ответ отменен: событие уже получено.");
                    callback.onResponse(call, Response.success(com.taxi.easy.ua.MainActivity.costMap));
                    return;
                }

                if (response.isSuccessful()) {
                    Log.d(TAG, "HTTP-ответ успешный: код " + response.code());

                    if (response.body() != null) {
                        Map<String, String> jsonResponse = response.body();

                        String orderCost = jsonResponse.get("order_cost");
                        String message = jsonResponse.getOrDefault("Message", "Нет сообщения от сервера");
                        Log.e(TAG, "orderCost" + orderCost);
                        Log.e(TAG, "message" + message);

                        if (!"0".equals(orderCost)) {
                            costMap.putAll(jsonResponse);
                            String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
                            sharedPreferencesHelperMain.saveValue(tarif, orderCost);
                        } else {
                            costMap.put("order_cost", "0");
                            costMap.put("Message", message);
                        }

                    } else {
                        Log.e(TAG, "Пустое тело ответа при успешном коде.");
                        costMap.put("order_cost", "0");
                        costMap.put("Message", "Пустой ответ от сервера");
                    }

                } else {
                    Log.e(TAG, "HTTP-ошибка: код " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Тело ошибки: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка чтения errorBody", e);
                    }
                    costMap.put("order_cost", "0");
                    costMap.put("Message", "Ошибка от сервера: " + response.code());
                }

                callback.onResponse(call, Response.success(costMap));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Log.e(TAG, "Ошибка выполнения запроса: " + t.getMessage(), t);

                Map<String, String> costMap = new HashMap<>();
                costMap.put("order_cost", "0");
                costMap.put("Message", "Ошибка подключения: " + t.getLocalizedMessage());

                callback.onResponse(call, Response.success(costMap));
            }

        });

        // твой поток — не трогаю
        new Thread(() -> {
            while (!eventReceived) {
                if (costMap != null && !costMap.isEmpty()) {
                    eventReceived = true;
                    if (activeCall != null && !activeCall.isExecuted()) {
                        activeCall.cancel();
                        Log.d("API_CALL", "HTTP-запрос прерван из-за события.");
                    }
                    callback.onResponse(call, Response.success(costMap));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}
