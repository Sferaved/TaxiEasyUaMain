package com.taxi.easy.ua.utils.from_json_parser;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

public class FromJSONParser {

    private static final String TAG = "FromJSONParser";

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

    public static Map<String, String> sendURL(String urlString) {
        Map<String, String> costMap = new HashMap<>();

        // Создание экземпляра OkHttpClient с таймаутом
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        // Создание экземпляра Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urlString) // базовый URL
                .client(okHttpClient) // настройка клиента
                .addConverterFactory(GsonConverterFactory.create()) // конвертер для парсинга JSON
                .build();

        // Создание объекта, представляющего интерфейс API
        ApiService apiService = retrofit.create(ApiService.class);

        // Выполнение запроса к API
        Call<ApiResponse> call = apiService.fetchData(urlString);

        try {
            retrofit2.Response<ApiResponse> response = call.execute();
            if (response.isSuccessful()) {
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
            Log.e(TAG, "Ошибка при выполнении запроса", e);
            costMap.put("order_cost", "0");
            costMap.put("message", "Произошла ошибка при выполнении запроса");
        }

        return costMap;
    }
}

