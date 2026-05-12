package com.taxi.easy.ua.utils.from_json_parser;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

public class FromJSONParserRetrofit {

    private static final String TAG = "FromJSONParserRetrofit";

    // ✅ ДОБАВИТЬ: Статическая переменная для хранения текущего запроса
    private static Call<ApiResponse> currentCall;

    // ✅ ДОБАВИТЬ: Флаг для отслеживания, отменен ли запрос
    private static boolean isCancelled = false;

    // Интерфейс для описания запросов к API
    public interface ApiService {
        @GET
        Call<ApiResponse> fetchData(@Url String url);
    }

    // Класс для представления ответа от сервера
    public static class ApiResponse {
        @SerializedName("order_cost")
        private String orderCost;

        @SerializedName("route_address_from")
        private String routeAddressFrom;

        @SerializedName("name")
        private String name;

        @SerializedName("house")
        private String house;

        @SerializedName("Message")
        private String message;

        public String getOrderCost() {
            return orderCost;
        }

        public String getRouteAddressFrom() {
            return routeAddressFrom;
        }

        public String getName() {
            return name;
        }

        public String getHouse() {
            return house;
        }

        public String getMessage() {
            return message;
        }
    }

    // ✅ ИЗМЕНИТЬ: Добавить возможность отмены
    public static void sendURL(String urlString, Callback<Map<String, String>> callback) {
        // Отменяем предыдущий запрос, если он существует и не выполнен
        cancelCurrentRequest();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        // ✅ Сбрасываем флаг отмены
        isCancelled = false;

        executor.execute(() -> {
            // ✅ Проверяем, не был ли запрос отменен
            if (isCancelled) {
                Log.d(TAG, "Request cancelled before execution");
                return;
            }

            Map<String, String> costMap = new HashMap<>();
            String baseUrl = getBaseUrl(urlString);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new RetryInterceptor())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);

            // ✅ Сохраняем текущий запрос
            currentCall = apiService.fetchData(urlString);

            try {
                // ✅ Проверяем отмену перед выполнением
                if (isCancelled) {
                    Log.d(TAG, "Request cancelled before execution");
                    return;
                }

                retrofit2.Response<ApiResponse> response = currentCall.execute();

                // ✅ Проверяем, не был ли запрос отменен во время выполнения
                if (isCancelled || currentCall.isCanceled()) {
                    Log.d(TAG, "Request was cancelled during execution");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse != null) {
                        if (!"0".equals(apiResponse.getOrderCost())) {
                            costMap.put("order_cost", "100");
                            costMap.put("route_address_from", apiResponse.getRouteAddressFrom());
                            costMap.put("name", apiResponse.getName());
                            costMap.put("house", apiResponse.getHouse());
                        } else {
                            costMap.put("order_cost", "0");
                            costMap.put("message", apiResponse.getMessage());
                        }
                    } else {
                        costMap.put("order_cost", "0");
                        costMap.put("message", "Ошибка: получен пустой ответ");
                    }
                } else {
                    costMap.put("order_cost", "0");
                    costMap.put("message", "Ошибка: " + response.code());
                }
            } catch (Exception e) {
                // ✅ Не логируем ошибку, если запрос был отменен
                if (!isCancelled && (currentCall == null || !currentCall.isCanceled())) {
                    Log.e(TAG, "Ошибка при выполнении запроса", e);
                    FirebaseCrashlytics.getInstance().recordException(e);
                    costMap.put("order_cost", "0");
                    costMap.put("message", "Произошла ошибка при выполнении запроса");
                } else {
                    Log.d(TAG, "Request cancelled, ignoring error");
                }
            } finally {
                // ✅ Очищаем текущий запрос, если это был он
                if (currentCall != null && !isCancelled) {
                    currentCall = null;
                }
            }

            // ✅ Передаем результат только если запрос не был отменен
            if (!isCancelled) {
                handler.post(() -> callback.onComplete(costMap));
            }

            executor.shutdown();
        });
    }

    // ✅ ДОБАВИТЬ: Метод для отмены текущего запроса
    public static void cancelCurrentRequest() {
        isCancelled = true;
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            Log.d(TAG, "Current request cancelled");
        }
        currentCall = null;
    }

    // Интерфейс обратного вызова для получения результата
    public interface Callback<T> {
        void onComplete(T result);
    }

    // Метод для получения базового URL из полной строки URL
    private static String getBaseUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + urlString, e);
        }
    }
}