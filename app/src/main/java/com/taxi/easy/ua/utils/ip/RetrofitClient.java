package com.taxi.easy.ua.utils.ip;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

//    private static final String BASE_URL = "https://m.easy-order-taxi.site/";
    private static final String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

    private static Retrofit retrofit = null;
    private static RetrofitClient instance;
    private OkHttpClient okHttpClient;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public void cancelAllRequests() {
        if (okHttpClient != null) {
            Dispatcher dispatcher = okHttpClient.dispatcher();
            dispatcher.cancelAll();
        }
    }
}
