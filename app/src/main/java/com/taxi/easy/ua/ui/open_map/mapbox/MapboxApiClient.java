package com.taxi.easy.ua.ui.open_map.mapbox;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapboxApiClient {

    private static final String BASE_URL = "https://api.mapbox.com/";

    public static MapboxService create() {
        // Создаем HTTP логгер
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Уровень логгирования

        // Создаем клиент OkHttp с логгированием
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        // Создаем Retrofit с настроенным клиентом
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(MapboxService.class);
    }
}

