package com.taxi.easy.ua.utils.worker.api;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import com.taxi.easy.ua.utils.city.BaseUrlHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static String lastBaseUrl = null;

    public static Retrofit getClient() {
        String baseUrl = BaseUrlHelper.fromPrefsWithSlash(sharedPreferencesHelperMain);
        if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new RetryInterceptor())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(logging)
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

    public static IPApiService getApiService() {
        return getClient().create(IPApiService.class);
    }
}
