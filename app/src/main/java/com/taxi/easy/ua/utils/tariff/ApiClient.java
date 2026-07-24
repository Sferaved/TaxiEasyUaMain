package com.taxi.easy.ua.utils.tariff;

import com.taxi.easy.ua.utils.city.BaseUrlHelper;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Lazy Retrofit: baseUrl from Firebase prefs (not captured at class load).
 */
public class ApiClient {
    private static Retrofit retrofit;
    private static String retrofitBaseUrl;

    public static Retrofit getRetrofitClient() {
        String baseUrl = BaseUrlHelper.fromPrefsWithSlash(sharedPreferencesHelperMain);
        if (baseUrl.isEmpty()) {
            return null;
        }
        String full = baseUrl + "apiTest/android/";
        if (retrofit == null || !full.equals(retrofitBaseUrl)) {
            retrofitBaseUrl = full;
            retrofit = new Retrofit.Builder()
                    .baseUrl(full)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
