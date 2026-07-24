package com.taxi.easy.ua.utils.permissions;

import com.taxi.easy.ua.utils.city.BaseUrlHelper;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientPermissions {
    private static final String BASE_URL = BaseUrlHelper.fromPrefs(sharedPreferencesHelperMain) + "/apiTest/";

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}