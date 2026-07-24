package com.taxi.easy.ua.ui.cities.api;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.util.Log;

import androidx.annotation.Nullable;

import com.taxi.easy.ua.utils.city.BaseUrlHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit-клиент городов. Не падает, если baseUrl ещё не пришёл из Firebase —
 * {@link #isReady()} / {@link #getClient()} вернут false/null.
 */
public class CityApiClient {

    private static final String TAG = "CityApiClient";

    @Nullable
    private final String baseUrl;

    public CityApiClient(@Nullable String baseUrl) {
        String normalized = BaseUrlHelper.normalize(baseUrl);
        if (!BaseUrlHelper.isValidHttpUrl(normalized)) {
            normalized = BaseUrlHelper.fromPrefs(sharedPreferencesHelperMain);
        }
        if (BaseUrlHelper.isValidHttpUrl(normalized)) {
            this.baseUrl = normalized.endsWith("/") ? normalized : normalized + "/";
        } else {
            this.baseUrl = null;
            Log.w(TAG, "baseUrl empty — wait keys/base_urls / city.base_url");
        }
    }

    public boolean isReady() {
        return baseUrl != null;
    }

    private Retrofit retrofit;

    @Nullable
    public Retrofit getClient() {
        if (baseUrl == null) {
            return null;
        }
        if (retrofit == null) {
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
        }
        return retrofit;
    }

    @Nullable
    public CityService createService() {
        Retrofit client = getClient();
        return client == null ? null : client.create(CityService.class);
    }
}
