package com.taxi.easy.ua.utils.to_json_parser;

import static com.taxi.easy.ua.ui.finish.FinishActivity.baseUrl;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ToJSONParserRetrofit {


    private APIService apiService;

    public ToJSONParserRetrofit() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(APIService.class);
    }

    public void sendURL(String urlString, final Callback<Map<String, String>> callback) {
        Call<JsonResponse> call = apiService.getData(urlString);

        call.enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(@NonNull Call<JsonResponse> call, @NonNull Response<JsonResponse> response) {
                Map<String, String> costMap = new HashMap<>();
                if (response.isSuccessful() && response.body() != null) {
                    JsonResponse jsonarray = response.body();

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
                    }
                } else {
                    costMap.put("order_cost", "0");
                    costMap.put("message", "Сталася помилка");
                }
                callback.onResponse(null, Response.success(costMap));
            }

            @Override
            public void onFailure(@NonNull Call<JsonResponse> call, @NonNull Throwable t) {
                Map<String, String> costMap = new HashMap<>();
                costMap.put("order_cost", "0");
                costMap.put("message", "Сталася помилка");
                callback.onResponse(null, Response.success(costMap));
            }
        });
    }
}