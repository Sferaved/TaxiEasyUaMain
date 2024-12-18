package com.taxi.easy.ua.utils.tariff;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit;
//    private static final String BASE_URL = "https://m.easy-order-taxi.site/apiTest/android/";
    private static final String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/apiTest/android/";

    public static Retrofit getRetrofitClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

