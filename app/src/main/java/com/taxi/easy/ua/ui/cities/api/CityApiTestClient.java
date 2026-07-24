package com.taxi.easy.ua.ui.cities.api;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import com.taxi.easy.ua.utils.city.BaseUrlHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CityApiTestClient {

    private static Retrofit retrofit;
    private static String lastBaseUrl;

    public static Retrofit getClient() {
        String baseUrl = BaseUrlHelper.fromPrefsWithSlash(sharedPreferencesHelperMain);
        if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new RetryInterceptor())
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            lastBaseUrl = baseUrl;
        }
        return retrofit;
    }
}
