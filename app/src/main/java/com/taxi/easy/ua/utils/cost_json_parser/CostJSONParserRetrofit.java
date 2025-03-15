package com.taxi.easy.ua.utils.cost_json_parser;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

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

    public void sendURL(String urlString, final Callback<Map<String, String>> callback) throws MalformedURLException {
        Call<Map<String, String>> call = apiService.getData(urlString);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
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
    }
}
