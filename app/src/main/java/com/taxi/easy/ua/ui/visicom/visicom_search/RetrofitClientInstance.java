package com.taxi.easy.ua.ui.visicom.visicom_search;

import android.util.Log;

import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientInstance {

    private static Retrofit retrofit;
    private static final String BASE_URL = "https://api.visicom.ua/data-api/5.0/";

    public static Retrofit getRetrofitInstance() {
        Log.d("Retrofit", "Entering getRetrofitInstance()");

        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new RetryInterceptor())
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                    .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                    .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL + LocaleHelper.getLocale() + "/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        Log.d("Retrofit", "Request URL: " + retrofit.baseUrl());
        Log.d("Locale", "Current Locale: " + LocaleHelper.getLocale());

        return retrofit;
    }
}
