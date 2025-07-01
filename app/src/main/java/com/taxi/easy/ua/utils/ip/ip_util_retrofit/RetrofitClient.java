package com.taxi.easy.ua.utils.ip.ip_util_retrofit;

import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) {
            // Создание логгера для HTTP запросов
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Создание клиента OkHttpClient с подключенным логгером и тайм-аутами
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(new RetryInterceptor())
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(60, TimeUnit.SECONDS) // Увеличиваем время ожидания подключения
                    .readTimeout(60, TimeUnit.SECONDS)    // Увеличиваем время ожидания чтения
                    .writeTimeout(60, TimeUnit.SECONDS)   // Увеличиваем время ожидания записи
                    .build();

            // Создание Retrofit с подключенным клиентом OkHttpClient
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
        }
        return retrofit;
    }
}
