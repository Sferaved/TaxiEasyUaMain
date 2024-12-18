package com.taxi.easy.ua.utils.to_json_parser;

import static com.taxi.easy.ua.MainActivity.activeCalls;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public class ToJSONParserRetrofit {


    private final APIService apiService;
    String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
    public ToJSONParserRetrofit() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        // Создайте interceptor для логирования
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Логирование тела запроса/ответа


        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(APIService.class);
    }

    public void sendURL(String urlString, final Callback<Map<String, String>> callback) {
        Log.d("API_CALL", "Sending URL: " + urlString); // Логируем URL

        Call<JsonResponse> call = apiService.getData(urlString);
        activeCalls.add(call);

        call.enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(@NonNull Call<JsonResponse> call, @NonNull Response<JsonResponse> response) {
                Map<String, String> costMap = new HashMap<>();
                Log.d("API_CALL", "Response received: " + response.toString()); // Логируем ответ

                if (response.isSuccessful() && response.body() != null) {
                    JsonResponse jsonarray = response.body();

                    Log.d("API_CALL", "Order cost: " + jsonarray.getOrderCost()); // Логируем стоимость заказа

                    if (!jsonarray.getOrderCost().equals("0")) {
                        costMap.put("from_lat", jsonarray.getFromLat());
                        costMap.put("from_lng", jsonarray.getFromLng());
                        costMap.put("lat", jsonarray.getLat());
                        costMap.put("lng", jsonarray.getLng());
                        costMap.put("dispatching_order_uid", jsonarray.getDispatchingOrderUid());
                        costMap.put("order_cost", jsonarray.getOrderCost());
                        costMap.put("currency", jsonarray.getCurrency());
                        costMap.put("routefrom", jsonarray.getRouteFrom());
                        costMap.put("routefromnumber", jsonarray.getRouteFromNumber());
                        costMap.put("routeto", jsonarray.getRouteTo());
                        costMap.put("to_number", jsonarray.getToNumber());
                        costMap.put("required_time", jsonarray.getRequired_time());
                        costMap.put("flexible_tariff_name", jsonarray.getFlexible_tariff_name());
                        costMap.put("comment_info", jsonarray.getComment_info());
                        costMap.put("extra_charge_codes", jsonarray.getExtra_charge_codes());

                        if (jsonarray.getDoubleOrder() != null) {
                            costMap.put("doubleOrder", jsonarray.getDoubleOrder());
                        }
                        if (jsonarray.getDispatchingOrderUidDouble() != null) {
                            costMap.put("dispatching_order_uid_Double", jsonarray.getDispatchingOrderUidDouble());
                        } else {
                            costMap.put("dispatching_order_uid_Double", " ");
                        }
                    } else {
                        costMap.put("order_cost", "0");
                        costMap.put("message", jsonarray.getMessage());
                        Log.d("API_CALL", "No cost found. Message: " + jsonarray.getMessage()); // Логируем сообщение
                    }
                } else {
                    costMap.put("order_cost", "0");
                    costMap.put("message", "Сталася помилка");
                    Log.e("API_CALL", "Error in response: " + response.message()); // Логируем ошибку ответа
                }
                callback.onResponse(null, Response.success(costMap));
            }

            @Override
            public void onFailure(@NonNull Call<JsonResponse> call, @NonNull Throwable t) {
                Map<String, String> costMap = new HashMap<>();
                costMap.put("order_cost", "0");
                costMap.put("message", "Сталася помилка");
                Log.e("API_CALL", "Request failed: " + t.getMessage()); // Логируем ошибку запроса
                callback.onResponse(null, Response.success(costMap));
            }
        });
    }

}