package com.taxi.easy.ua.ui.finish;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import com.taxi.easy.ua.utils.network.ApiGsonHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
//    static final String BASE_URL = "https://m.easy-order-taxi.site/";
    static String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

    private static Retrofit retrofit = null;
    private static Retrofit cancelRetrofit = null;
    private static Retrofit pollingRetrofit = null;

    public static ApiService getApiService() {
        //Логирование****
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor()) // 3 попытки
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут подключения
                .writeTimeout(30, TimeUnit.SECONDS)  // Тайм-аут записи
                .readTimeout(30, TimeUnit.SECONDS)   // Тайм-аут чтения
                .build();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(ApiGsonHelper.create()))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    /** Отмена заказа — одна попытка без RetryInterceptor, чтобы не дублировать cancel на сервере. */
    public static ApiService getCancelApiService() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        if (cancelRetrofit == null) {
            cancelRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(ApiGsonHelper.create()))
                    .build();
        }
        return cancelRetrofit.create(ApiService.class);
    }

    /** Фоновый опрос статуса заказа — без retry, таймауты согласованы с OrderStatusUtils (15 с). */
    public static ApiService getPollingApiService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build();

        if (pollingRetrofit == null) {
            pollingRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(ApiGsonHelper.create()))
                    .build();
        }
        return pollingRetrofit.create(ApiService.class);
    }
}
